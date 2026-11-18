package com.crm.core.dto.lead;

import com.crm.core.entity.LeadEvent;
import com.crm.core.entity.enums.LeadEventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadEventView(
        UUID id,
        LeadEventType type,
        UUID fromStageId,
        UUID toStageId,
        UUID managerId,
        UUID lostReasonId,
        String comment,
        UUID actorId,
        LocalDateTime createdAt
) {
    public static LeadEventView of(LeadEvent e) {
        return new LeadEventView(e.getId(), e.getType(), e.getFromStageId(), e.getToStageId(), e.getManagerId(),
                e.getLostReasonId(), e.getComment(), e.getActorId(), e.getCreatedAt());
    }
}
