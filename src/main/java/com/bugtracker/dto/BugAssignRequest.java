package com.bugtracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BugAssignRequest {

    @NotNull
    private Long developerId;
}
