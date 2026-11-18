package com.crm.core.service;

import com.crm.core.dto.task.CreateTaskRequest;
import com.crm.core.dto.task.TaskSummary;
import com.crm.core.dto.task.TaskView;
import com.crm.core.dto.task.UpdateTaskRequest;
import com.crm.core.entity.CrmTask;
import com.crm.core.entity.enums.StaffRole;
import com.crm.core.entity.enums.TaskStatus;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.LeadRepository;
import com.crm.core.repository.PersonRepository;
import com.crm.core.repository.TaskRepository;
import com.crm.core.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Задачи и напоминания сотрудникам. Срок и «просрочено» — по часовому поясу школы. */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final LeadRepository leadRepository;
    private final PersonRepository personRepository;
    private final UserService userService;
    private final OrganizationClock clock;

    /**
     * @param assigneeId null вместе с allAssignees=false — задачи текущего пользователя
     * @param status     null — любой статус
     */
    @Transactional(readOnly = true)
    public List<TaskView> search(UUID assigneeId, boolean allAssignees, TaskStatus status, UUID leadId, UUID personId) {
        UUID assignee = allAssignees ? null : (assigneeId != null ? assigneeId : CurrentUser.get().id());
        LocalDateTime now = clock.now();
        return taskRepository.search(assignee, status, leadId, personId).stream()
                .map(task -> TaskView.of(task, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskSummary mySummary() {
        UUID me = CurrentUser.get().id();
        LocalDateTime now = clock.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        return new TaskSummary(
                taskRepository.countByAssigneeIdAndStatus(me, TaskStatus.OPEN),
                taskRepository.countByAssigneeIdAndStatusAndDueAtBefore(me, TaskStatus.OPEN, now),
                // «На сегодня» — срок сегодня и ещё не прошёл
                taskRepository.countByAssigneeIdAndStatusAndDueAtBetween(me, TaskStatus.OPEN, now,
                        startOfDay.plusDays(1).minusNanos(1)));
    }

    @Transactional
    public TaskView create(CreateTaskRequest request) {
        UUID me = CurrentUser.get().id();
        UUID assignee = request.assigneeId() == null ? me : requireAssignee(request.assigneeId());
        if (request.leadId() != null && !leadRepository.existsById(request.leadId())) {
            throw new NotFoundException("Лид не найден");
        }
        if (request.personId() != null && !personRepository.existsById(request.personId())) {
            throw new NotFoundException("Клиент не найден");
        }

        CrmTask task = taskRepository.saveAndFlush(CrmTask.builder()
                .title(request.title().trim())
                .description(blankToNull(request.description()))
                .dueAt(request.dueAt())
                .status(TaskStatus.OPEN)
                .assigneeId(assignee)
                .createdBy(me)
                .leadId(request.leadId())
                .personId(request.personId())
                .build());
        return TaskView.of(task, clock.now());
    }

    @Transactional
    public TaskView update(UUID id, UpdateTaskRequest request) {
        CrmTask task = findEditable(id);
        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setDueAt(request.dueAt());
        task.setAssigneeId(requireAssignee(request.assigneeId()));
        return TaskView.of(task, clock.now());
    }

    @Transactional
    public TaskView changeStatus(UUID id, TaskStatus status) {
        CrmTask task = findEditable(id);
        task.setStatus(status);
        if (status == TaskStatus.OPEN) {
            task.setCompletedAt(null);
            task.setCompletedBy(null);
        } else {
            task.setCompletedAt(clock.now());
            task.setCompletedBy(CurrentUser.get().id());
        }
        return TaskView.of(task, clock.now());
    }

    /** Изменять задачу могут исполнитель, автор и администратор школы. */
    private CrmTask findEditable(UUID id) {
        CrmTask task = taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Задача не найдена"));
        CurrentUser user = CurrentUser.get();
        boolean allowed = user.hasRole(StaffRole.ADMIN)
                || user.id().equals(task.getAssigneeId())
                || user.id().equals(task.getCreatedBy());
        if (!allowed) {
            throw new AccessDeniedException("Изменять задачу могут исполнитель, автор и администратор");
        }
        return task;
    }

    private UUID requireAssignee(UUID userId) {
        return userService.requireActiveStaff(userId, EnumSet.allOf(StaffRole.class),
                "Исполнитель должен быть активным сотрудником школы").getId();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
