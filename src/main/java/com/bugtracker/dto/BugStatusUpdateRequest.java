package com.bugtracker.dto;

import com.bugtracker.entity.BugStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BugStatusUpdateRequest {

    @NotNull
    private BugStatus status;
}
