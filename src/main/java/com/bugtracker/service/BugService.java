package com.bugtracker.service;

import com.bugtracker.dto.BugRequest;
import com.bugtracker.dto.BugResponse;
import com.bugtracker.entity.*;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.repository.AuditLogRepository;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BugService {

    private final BugRepository bugRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    // ----- CREATE (Tester or Admin reports a bug) -----
    public BugResponse createBug(BugRequest request, User currentUser) {
        if (currentUser.getRole() != Role.TESTER && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only Testers or Admins can report bugs");
        }

        Bug bug = Bug.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(BugStatus.OPEN)
                .reportedBy(currentUser)
                .build();

        Bug saved = bugRepository.save(bug);

        logAudit(saved.getId(), "CREATED", "Bug reported with priority " + saved.getPriority(), currentUser.getUsername());

        return toResponse(saved);
    }

    // ----- READ -----
    public List<BugResponse> getAllBugs() {
        return bugRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public BugResponse getBugById(Long id) {
        Bug bug = findBugOrThrow(id);
        return toResponse(bug);
    }

    public List<BugResponse> getBugsAssignedToMe(Long userId) {
        return bugRepository.findByAssignedTo_Id(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BugResponse> getBugsReportedByMe(Long userId) {
        return bugRepository.findByReportedBy_Id(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BugResponse> getBugsByStatus(BugStatus status) {
        return bugRepository.findByStatus(status).stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ----- ASSIGN (Admin assigns bug to a Developer) -----
    public BugResponse assignBug(Long bugId, Long developerId, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only Admins can assign bugs to developers");
        }

        Bug bug = findBugOrThrow(bugId);

        User developer = userRepository.findById(developerId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer not found with id: " + developerId));

        if (developer.getRole() != Role.DEVELOPER) {
            throw new IllegalArgumentException("Bugs can only be assigned to users with DEVELOPER role");
        }

        bug.setAssignedTo(developer);
        if (bug.getStatus() == BugStatus.OPEN) {
            bug.setStatus(BugStatus.IN_PROGRESS);
        }

        Bug updated = bugRepository.save(bug);

        logAudit(bugId, "ASSIGNED", "Assigned to developer: " + developer.getUsername(), currentUser.getUsername());

        return toResponse(updated);
    }

    // ----- STATUS UPDATE (Developer updates progress, Tester verifies/reopens, Admin can close) -----
    public BugResponse updateStatus(Long bugId, BugStatus newStatus, User currentUser) {
        Bug bug = findBugOrThrow(bugId);

        validateStatusTransition(bug, newStatus, currentUser);

        BugStatus oldStatus = bug.getStatus();
        bug.setStatus(newStatus);
        Bug updated = bugRepository.save(bug);

        logAudit(bugId, "STATUS_CHANGED", oldStatus + " -> " + newStatus, currentUser.getUsername());

        return toResponse(updated);
    }

    private void validateStatusTransition(Bug bug, BugStatus newStatus, User currentUser) {
        Role role = currentUser.getRole();

        boolean isAssignedDeveloper = bug.getAssignedTo() != null
                && bug.getAssignedTo().getId().equals(currentUser.getId());
        boolean isReporter = bug.getReportedBy().getId().equals(currentUser.getId());

        switch (newStatus) {
            case IN_PROGRESS -> {
                if (role != Role.DEVELOPER && role != Role.ADMIN) {
                    throw new AccessDeniedException("Only the assigned Developer or Admin can move a bug to IN_PROGRESS");
                }
                if (role == Role.DEVELOPER && !isAssignedDeveloper) {
                    throw new AccessDeniedException("Only the developer assigned to this bug can update it");
                }
            }
            case RESOLVED -> {
                if (role != Role.DEVELOPER && role != Role.ADMIN) {
                    throw new AccessDeniedException("Only the assigned Developer or Admin can mark a bug RESOLVED");
                }
                if (role == Role.DEVELOPER && !isAssignedDeveloper) {
                    throw new AccessDeniedException("Only the developer assigned to this bug can resolve it");
                }
            }
            case REOPENED -> {
                if (role != Role.TESTER && role != Role.ADMIN) {
                    throw new AccessDeniedException("Only the Tester who reported the bug or Admin can reopen it");
                }
                if (role == Role.TESTER && !isReporter) {
                    throw new AccessDeniedException("Only the reporting Tester can reopen this bug");
                }
            }
            case CLOSED -> {
                if (role != Role.TESTER && role != Role.ADMIN) {
                    throw new AccessDeniedException("Only the Tester who reported the bug or Admin can close it");
                }
                if (role == Role.TESTER && !isReporter) {
                    throw new AccessDeniedException("Only the reporting Tester can close this bug");
                }
            }
            case OPEN -> {
                if (role != Role.ADMIN) {
                    throw new AccessDeniedException("Only Admin can reset a bug back to OPEN");
                }
            }
        }
    }

    // ----- DELETE (Admin only) -----
    public void deleteBug(Long bugId, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only Admins can delete bugs");
        }
        Bug bug = findBugOrThrow(bugId);
        bugRepository.delete(bug);
        logAudit(bugId, "DELETED", "Bug deleted", currentUser.getUsername());
    }

    // ----- Helpers -----
    private Bug findBugOrThrow(Long id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + id));
    }

    private void logAudit(Long bugId, String action, String details, String performedBy) {
        AuditLog log = AuditLog.builder()
                .bugId(bugId)
                .action(action)
                .details(details)
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(log);
    }

    private BugResponse toResponse(Bug bug) {
        return BugResponse.builder()
                .id(bug.getId())
                .title(bug.getTitle())
                .description(bug.getDescription())
                .status(bug.getStatus())
                .priority(bug.getPriority())
                .reportedByUsername(bug.getReportedBy() != null ? bug.getReportedBy().getUsername() : null)
                .assignedToUsername(bug.getAssignedTo() != null ? bug.getAssignedTo().getUsername() : null)
                .createdAt(bug.getCreatedAt())
                .updatedAt(bug.getUpdatedAt())
                .build();
    }
}
