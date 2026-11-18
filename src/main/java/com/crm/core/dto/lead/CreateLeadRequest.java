package com.crm.core.dto.lead;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Новый лид. pipelineId/stageId необязательны — по умолчанию первый этап основной воронки.
 * managerId необязателен — по умолчанию лид назначается создавшему его менеджеру.
 */
public record CreateLeadRequest(
        @NotBlank String name,
        String phoneNumber,
        String messengerId,
        String source,
        UUID pipelineId,
        UUID stageId,
        UUID managerId
) {}
