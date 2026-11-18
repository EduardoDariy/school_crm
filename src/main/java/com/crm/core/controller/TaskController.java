package com.crm.core.controller;

import com.crm.core.dto.task.CreateTaskRequest;
import com.crm.core.dto.task.TaskSummary;
import com.crm.core.dto.task.TaskView;
import com.crm.core.dto.task.UpdateTaskRequest;
import com.crm.core.entity.enums.TaskStatus;
import com.crm.core.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Задачи", description = "Задачи и напоминания; администратор, менеджер")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Список задач",
            description = "По умолчанию — мои открытые. all=true — всех сотрудников; status пусто + anyStatus=true — любые.")
    @GetMapping
    public List<TaskView> search(@RequestParam(required = false) UUID assigneeId,
                                 @RequestParam(defaultValue = "false") boolean all,
                                 @RequestParam(required = false) TaskStatus status,
                                 @RequestParam(defaultValue = "false") boolean anyStatus,
                                 @RequestParam(required = false) UUID leadId,
                                 @RequestParam(required = false) UUID personId) {
        TaskStatus effectiveStatus = anyStatus ? null : (status != null ? status : TaskStatus.OPEN);
        // Задачи по конкретному лиду/клиенту показываем всех исполнителей
        boolean allAssignees = all || leadId != null || personId != null;
        return taskService.search(assigneeId, allAssignees && assigneeId == null, effectiveStatus, leadId, personId);
    }

    @Operation(summary = "Сводка «Мои задачи»: открытые, просроченные, на сегодня")
    @GetMapping("/summary")
    public TaskSummary summary() {
        return taskService.mySummary();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskView create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @Operation(summary = "Изменить задачу", description = "Исполнитель, автор или администратор.")
    @PutMapping("/{id}")
    public TaskView update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @PostMapping("/{id}/complete")
    public TaskView complete(@PathVariable UUID id) {
        return taskService.changeStatus(id, TaskStatus.DONE);
    }

    @PostMapping("/{id}/cancel")
    public TaskView cancel(@PathVariable UUID id) {
        return taskService.changeStatus(id, TaskStatus.CANCELLED);
    }

    @PostMapping("/{id}/reopen")
    public TaskView reopen(@PathVariable UUID id) {
        return taskService.changeStatus(id, TaskStatus.OPEN);
    }
}
