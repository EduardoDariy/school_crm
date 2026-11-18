package com.crm.core.entity;

import com.crm.core.entity.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Задача/напоминание сотруднику, при необходимости по лиду или клиенту.
 * Называется CrmTask, чтобы не путать с java.util.concurrent и Spring Task.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmTask extends BaseEntity {

    @Column(nullable = false)
    private String title;

    private String description;

    /** Срок в часовом поясе организации. */
    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;
}
