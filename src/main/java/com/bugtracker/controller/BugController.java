package com.bugtracker.controller;

import com.bugtracker.dto.*;
import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.User;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.repository.UserRepository;
import com.bugtracker.security.UserDetailsImpl;
import com.bugtracker.service.BugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugController {

    private final BugService bugService;
    private final UserRepository userRepository;

    // ---- Create a bug: Tester or Admin ----
    @PostMapping
    public ResponseEntity<BugResponse> createBug(@Valid @RequestBody BugRequest request) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(bugService.createBug(request, currentUser));
    }

    // ---- Get all bugs: any authenticated user ----
    @GetMapping
    public ResponseEntity<List<BugResponse>> getAllBugs() {
        return ResponseEntity.ok(bugService.getAllBugs());
    }

    // ---- Get single bug by id ----
    @GetMapping("/{id}")
    public ResponseEntity<BugResponse> getBugById(@PathVariable Long id) {
        return ResponseEntity.ok(bugService.getBugById(id));
    }

    // ---- Get bugs assigned to the currently logged in Developer ----
    @GetMapping("/assigned-to-me")
    public ResponseEntity<List<BugResponse>> getBugsAssignedToMe() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(bugService.getBugsAssignedToMe(currentUser.getId()));
    }

    // ---- Get bugs reported by the currently logged in Tester ----
    @GetMapping("/reported-by-me")
    public ResponseEntity<List<BugResponse>> getBugsReportedByMe() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(bugService.getBugsReportedByMe(currentUser.getId()));
    }

    // ---- Filter bugs by status ----
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BugResponse>> getBugsByStatus(@PathVariable BugStatus status) {
        return ResponseEntity.ok(bugService.getBugsByStatus(status));
    }

    // ---- Assign bug to a developer: Admin only ----
    @PutMapping("/{id}/assign")
    public ResponseEntity<BugResponse> assignBug(@PathVariable Long id, @Valid @RequestBody BugAssignRequest request) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(bugService.assignBug(id, request.getDeveloperId(), currentUser));
    }

    // ---- Update bug status: workflow governed by BugService ----
    @PutMapping("/{id}/status")
    public ResponseEntity<BugResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody BugStatusUpdateRequest request) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(bugService.updateStatus(id, request.getStatus(), currentUser));
    }

    // ---- Delete bug: Admin only ----
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteBug(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        bugService.deleteBug(id, currentUser);
        return ResponseEntity.ok(new MessageResponse("Bug deleted successfully"));
    }

    // ---- Helper: fetch full User entity for the logged-in principal ----
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found"));
    }
}
