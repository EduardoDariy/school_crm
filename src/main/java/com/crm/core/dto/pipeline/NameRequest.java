package com.crm.core.dto.pipeline;

import jakarta.validation.constraints.NotBlank;

/** Название этапа или причины проигрыша. */
public record NameRequest(@NotBlank String name) {
}
