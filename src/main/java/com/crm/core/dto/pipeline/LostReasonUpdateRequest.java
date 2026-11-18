package com.crm.core.dto.pipeline;

import jakarta.validation.constraints.NotBlank;

public record LostReasonUpdateRequest(@NotBlank String name, boolean active) {
}
