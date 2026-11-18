package com.crm.core.dto.lead;

import java.util.UUID;

/** Причина обязательна или нет — зависит от настройки воронки. */
public record LoseLeadRequest(UUID lostReasonId, String comment) {
}
