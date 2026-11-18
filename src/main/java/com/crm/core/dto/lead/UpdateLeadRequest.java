package com.crm.core.dto.lead;

import jakarta.validation.constraints.NotBlank;

/** Контактные данные лида. */
public record UpdateLeadRequest(
        @NotBlank String name,
        String phoneNumber,
        String messengerId,
        String source
) {}
