package com.crm.core.dto.lead;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveStageRequest(@NotNull UUID stageId) {
}
