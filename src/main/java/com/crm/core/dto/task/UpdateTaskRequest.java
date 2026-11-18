package com.crm.core.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime dueAt,
        @NotNull UUID assigneeId
) {}
