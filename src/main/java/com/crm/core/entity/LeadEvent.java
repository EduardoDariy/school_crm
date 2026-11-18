package com.crm.core.entity;

import com.crm.core.entity.enums.LeadEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Запись истории лида. Неизменяемая: только добавление. */
@Entity
@Immutable
@Table(name = "lead_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadEvent extends BaseEntity {

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadEventType type;

    @Column(name = "from_stage_id")
    private UUID fromStageId;

    @Column(name = "to_stage_id")
    private UUID toStageId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "lost_reason_id")
    private UUID lostReasonId;

    private String comment;

    /** Кто выполнил действие; пусто для автоматических действий системы. */
    @Column(name = "actor_id")
    private UUID actorId;
}
