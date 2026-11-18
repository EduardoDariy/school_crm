package com.crm.core.controller;

import com.crm.core.dto.lead.*;
import com.crm.core.entity.enums.LeadStatus;
import com.crm.core.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Лиды", description = "Лиды и движение по воронке продаж; администратор, менеджер")
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class LeadController {

    private final LeadService leadService;

    @Operation(summary = "Список лидов",
            description = "Все фильтры необязательны. mine=true — только мои; query — поиск по имени, телефону, мессенджеру.")
    @GetMapping
    public List<LeadView> search(@RequestParam(required = false) UUID pipelineId,
                                 @RequestParam(required = false) UUID stageId,
                                 @RequestParam(required = false) LeadStatus status,
                                 @RequestParam(required = false) UUID managerId,
                                 @RequestParam(defaultValue = "false") boolean mine,
                                 @RequestParam(defaultValue = "false") boolean archived,
                                 @RequestParam(required = false) String query) {
        return leadService.search(new LeadService.Filter(pipelineId, stageId, status, managerId, mine, archived, query));
    }

    @Operation(summary = "Создать лид",
            description = "Дубль по телефону или мессенджеру → 409 с existingId существующей карточки.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadView create(@Valid @RequestBody CreateLeadRequest request) {
        return leadService.create(request);
    }

    @GetMapping("/{id}")
    public LeadView get(@PathVariable UUID id) {
        return leadService.get(id);
    }

    @Operation(summary = "Изменить контактные данные")
    @PutMapping("/{id}")
    public LeadView update(@PathVariable UUID id, @Valid @RequestBody UpdateLeadRequest request) {
        return leadService.update(id, request);
    }

    @Operation(summary = "История лида", description = "Создание, смены этапов и ответственного, итог.")
    @GetMapping("/{id}/history")
    public List<LeadEventView> history(@PathVariable UUID id) {
        return leadService.history(id);
    }

    @Operation(summary = "Перевести на этап воронки")
    @PostMapping("/{id}/stage")
    public LeadView moveToStage(@PathVariable UUID id, @Valid @RequestBody MoveStageRequest request) {
        return leadService.moveToStage(id, request.stageId());
    }

    @Operation(summary = "Назначить ответственного менеджера")
    @PostMapping("/{id}/manager")
    public LeadView assignManager(@PathVariable UUID id, @RequestBody AssignManagerRequest request) {
        return leadService.assignManager(id, request.managerId());
    }

    @Operation(summary = "Успешная продажа", description = "Закрывает лид и создаёт (или находит) карточку ученика.")
    @PostMapping("/{id}/win")
    public LeadView win(@PathVariable UUID id) {
        return leadService.win(id);
    }

    @Operation(summary = "Проигрыш", description = "Причина обязательна, если так настроена воронка.")
    @PostMapping("/{id}/lose")
    public LeadView lose(@PathVariable UUID id, @RequestBody(required = false) LoseLeadRequest request) {
        return leadService.lose(id, request);
    }

    @Operation(summary = "Вернуть закрытый лид в работу")
    @PostMapping("/{id}/reopen")
    public LeadView reopen(@PathVariable UUID id) {
        return leadService.reopen(id);
    }

    @Operation(summary = "В архив", description = "Вместо удаления: история сохраняется, дубли находятся.")
    @PostMapping("/{id}/archive")
    public LeadView archive(@PathVariable UUID id) {
        return leadService.setArchived(id, true);
    }

    @PostMapping("/{id}/unarchive")
    public LeadView unarchive(@PathVariable UUID id) {
        return leadService.setArchived(id, false);
    }
}
