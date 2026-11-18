package com.crm.core.entity;

import com.crm.core.entity.enums.LeadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Нормализованный номер: «+» и цифры (см. PhoneNumbers). */
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "messenger_id")
    private String messengerId;

    /** Рекламный источник/канал обращения: Instagram, сайт, рекомендация и т. п. */
    @Column(name = "source")
    private String source;

    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    /** Текущий этап; после выигрыша/проигрыша остаётся последним этапом, на котором был лид. */
    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    /** Ответственный менеджер (сотрудник школы). */
    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "lost_reason_id")
    private UUID lostReasonId;

    @Column(name = "lost_comment")
    private String lostComment;

    /** Карточка человека, созданная при выигрыше лида. */
    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Архив вместо удаления: история сохраняется, повторное обращение находит карточку. */
    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
