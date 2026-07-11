package com.bugtracker.dto;

import com.bugtracker.entity.BugPriority;
import com.bugtracker.entity.BugStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugResponse {
    private Long id;
    private String title;
    private String description;
    private BugStatus status;
    private BugPriority priority;
    private String reportedByUsername;
    private String assignedToUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
