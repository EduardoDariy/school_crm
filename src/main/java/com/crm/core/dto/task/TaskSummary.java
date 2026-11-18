package com.crm.core.dto.task;

/** Сводка для виджета «Мои задачи». */
public record TaskSummary(long open, long overdue, long dueToday) {
}
