package com.crm.core.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/** assigneeId необязателен — по умолчанию задача ставится себе. dueAt — время школы. */
public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime dueAt,
        UUID assigneeId,
        UUID leadId,
        UUID personId
) {}
