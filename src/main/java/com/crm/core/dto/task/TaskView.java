package com.crm.core.dto.task;

import com.crm.core.entity.CrmTask;
import com.crm.core.entity.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** overdue — открытая задача с прошедшим сроком (по часовому поясу школы). */
public record TaskView(
        UUID id,
        String title,
        String description,
        LocalDateTime dueAt,
        TaskStatus status,
        boolean overdue,
        UUID assigneeId,
        UUID createdBy,
        UUID leadId,
        UUID personId,
        LocalDateTime completedAt,
        UUID completedBy,
        LocalDateTime createdAt
) {
    public static TaskView of(CrmTask t, LocalDateTime now) {
        boolean overdue = t.getStatus() == TaskStatus.OPEN && t.getDueAt().isBefore(now);
        return new TaskView(t.getId(), t.getTitle(), t.getDescription(), t.getDueAt(), t.getStatus(), overdue,
                t.getAssigneeId(), t.getCreatedBy(), t.getLeadId(), t.getPersonId(), t.getCompletedAt(),
                t.getCompletedBy(), t.getCreatedAt());
    }
}
