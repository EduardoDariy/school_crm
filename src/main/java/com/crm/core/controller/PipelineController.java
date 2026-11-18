package com.crm.core.controller;

import com.crm.core.dto.pipeline.*;
import com.crm.core.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Воронки", description = "Настройка воронок продаж: просмотр — менеджер, изменение — администратор")
@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PipelineController {

    private final PipelineService pipelineService;

    @Operation(summary = "Воронки с этапами и причинами проигрыша")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<PipelineView> list() {
        return pipelineService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PipelineView get(@PathVariable UUID id) {
        return pipelineService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PipelineView create(@Valid @RequestBody CreatePipelineRequest request) {
        return pipelineService.create(request);
    }

    @Operation(summary = "Название и режим причин проигрыша (DISABLED, OPTIONAL, REQUIRED)")
    @PutMapping("/{id}")
    public PipelineView update(@PathVariable UUID id, @Valid @RequestBody UpdatePipelineRequest request) {
        return pipelineService.update(id, request);
    }

    @Operation(summary = "Добавить этап в конец воронки")
    @PostMapping("/{id}/stages")
    public PipelineView addStage(@PathVariable UUID id, @Valid @RequestBody NameRequest request) {
        return pipelineService.addStage(id, request.name());
    }

    @PutMapping("/{id}/stages/{stageId}")
    public PipelineView renameStage(@PathVariable UUID id, @PathVariable UUID stageId,
                                    @Valid @RequestBody NameRequest request) {
        return pipelineService.renameStage(id, stageId, request.name());
    }

    @Operation(summary = "Изменить порядок этапов", description = "Передаются все этапы воронки в новом порядке.")
    @PutMapping("/{id}/stages/order")
    public PipelineView reorderStages(@PathVariable UUID id, @Valid @RequestBody ReorderRequest request) {
        return pipelineService.reorderStages(id, request.stageIds());
    }

    @Operation(summary = "Удалить этап", description = "Если на этапе есть лиды — обязателен moveToStageId.")
    @DeleteMapping("/{id}/stages/{stageId}")
    public PipelineView deleteStage(@PathVariable UUID id, @PathVariable UUID stageId,
                                    @RequestParam(required = false) UUID moveToStageId) {
        return pipelineService.deleteStage(id, stageId, moveToStageId);
    }

    @PostMapping("/{id}/lost-reasons")
    public PipelineView addLostReason(@PathVariable UUID id, @Valid @RequestBody NameRequest request) {
        return pipelineService.addLostReason(id, request.name());
    }

    @Operation(summary = "Переименовать или выключить причину проигрыша")
    @PutMapping("/{id}/lost-reasons/{reasonId}")
    public PipelineView updateLostReason(@PathVariable UUID id, @PathVariable UUID reasonId,
                                         @Valid @RequestBody LostReasonUpdateRequest request) {
        return pipelineService.updateLostReason(id, reasonId, request);
    }
}
