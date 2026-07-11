package com.bugtracker.dto;

import com.bugtracker.entity.BugPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BugRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private BugPriority priority;
}
