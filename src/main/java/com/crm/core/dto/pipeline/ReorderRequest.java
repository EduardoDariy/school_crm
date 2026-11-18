package com.crm.core.dto.pipeline;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Все этапы воронки в новом порядке. */
public record ReorderRequest(@NotEmpty List<UUID> stageIds) {
}
