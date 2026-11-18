package com.crm.core.dto.lead;

import com.crm.core.entity.Lead;
import com.crm.core.entity.enums.LeadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeadView(
        UUID id,
        String name,
        String phoneNumber,
        String messengerId,
        String source,
        UUID pipelineId,
        UUID stageId,
        LeadStatus status,
        UUID managerId,
        UUID lostReasonId,
        String lostComment,
        UUID personId,
        boolean archived,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LeadView of(Lead lead) {
        return new LeadView(lead.getId(), lead.getName(), lead.getPhoneNumber(), lead.getMessengerId(),
                lead.getSource(), lead.getPipelineId(), lead.getStageId(), lead.getStatus(), lead.getManagerId(),
                lead.getLostReasonId(), lead.getLostComment(), lead.getPersonId(), lead.isArchived(),
                lead.getClosedAt(), lead.getCreatedAt(), lead.getUpdatedAt());
    }
}
