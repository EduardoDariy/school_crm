package com.crm.core.service;

import com.crm.core.dto.pipeline.*;
import com.crm.core.entity.LostReason;
import com.crm.core.entity.Pipeline;
import com.crm.core.entity.PipelineStage;
import com.crm.core.entity.enums.LostReasonMode;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.LeadRepository;
import com.crm.core.repository.LostReasonRepository;
import com.crm.core.repository.PipelineRepository;
import com.crm.core.repository.PipelineStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Воронки продаж текущей организации: этапы и причины проигрыша. */
@Service
@RequiredArgsConstructor
public class PipelineService {

    /** Стартовая воронка новой школы. Те же значения — в миграции V9 для существующих школ. */
    static final String DEFAULT_NAME = "Продажи";
    static final List<String> DEFAULT_STAGES =
            List.of("Новое обращение", "Взят в работу", "Пробное занятие", "Принимает решение");
    static final List<String> DEFAULT_LOST_REASONS =
            List.of("Дорого", "Не подошло расписание", "Не выходит на связь", "Выбрал другую школу", "Другое");

    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;
    private final LostReasonRepository lostReasonRepository;
    private final LeadRepository leadRepository;

    @Transactional(readOnly = true)
    public List<PipelineView> list() {
        List<Pipeline> pipelines = pipelineRepository.findAllByArchivedFalseOrderByCreatedAtAsc();
        List<UUID> ids = pipelines.stream().map(Pipeline::getId).toList();
        Map<UUID, List<PipelineStage>> stages = stageRepository.findAllByPipelineIdInAndArchivedFalseOrderByPositionAsc(ids)
                .stream().collect(Collectors.groupingBy(PipelineStage::getPipelineId));
        Map<UUID, List<LostReason>> reasons = lostReasonRepository.findAllByPipelineIdInOrderByPositionAsc(ids)
                .stream().collect(Collectors.groupingBy(LostReason::getPipelineId));
        return pipelines.stream()
                .map(p -> PipelineView.of(p, stages.getOrDefault(p.getId(), List.of()),
                        reasons.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PipelineView get(UUID pipelineId) {
        return view(requirePipeline(pipelineId));
    }

    /** Создаёт стартовую воронку для новой организации (вызывается в контексте этой организации). */
    @Transactional
    public PipelineView createDefault() {
        return pipelineRepository.findByDefaultPipelineTrue()
                .map(this::view)
                .orElseGet(() -> view(createPipeline(DEFAULT_NAME, LostReasonMode.OPTIONAL, true,
                        DEFAULT_STAGES, DEFAULT_LOST_REASONS)));
    }

    @Transactional
    public PipelineView create(CreatePipelineRequest request) {
        boolean isFirst = pipelineRepository.findByDefaultPipelineTrue().isEmpty();
        LostReasonMode mode = request.lostReasonMode() == null ? LostReasonMode.OPTIONAL : request.lostReasonMode();
        return view(createPipeline(request.name().trim(), mode, isFirst, request.stages(),
                request.lostReasons() == null ? List.of() : request.lostReasons()));
    }

    @Transactional
    public PipelineView update(UUID pipelineId, UpdatePipelineRequest request) {
        Pipeline pipeline = requirePipeline(pipelineId);
        pipeline.setName(request.name().trim());
        pipeline.setLostReasonMode(request.lostReasonMode());
        return view(pipeline);
    }

    // ---------- Этапы ----------

    @Transactional
    public PipelineView addStage(UUID pipelineId, String name) {
        Pipeline pipeline = requirePipeline(pipelineId);
        int position = activeStages(pipelineId).size();
        stageRepository.save(PipelineStage.builder().pipelineId(pipelineId).name(name.trim()).position(position).build());
        return view(pipeline);
    }

    @Transactional
    public PipelineView renameStage(UUID pipelineId, UUID stageId, String name) {
        requireStage(pipelineId, stageId).setName(name.trim());
        return view(requirePipeline(pipelineId));
    }

    @Transactional
    public PipelineView reorderStages(UUID pipelineId, List<UUID> stageIds) {
        Pipeline pipeline = requirePipeline(pipelineId);
        Map<UUID, PipelineStage> stages = activeStages(pipelineId).stream()
                .collect(Collectors.toMap(PipelineStage::getId, Function.identity()));
        if (stageIds.size() != stages.size() || !stages.keySet().equals(new HashSet<>(stageIds))) {
            throw new IllegalArgumentException("Нужно передать все этапы воронки ровно по одному разу");
        }
        for (int i = 0; i < stageIds.size(); i++) {
            stages.get(stageIds.get(i)).setPosition(i);
        }
        return view(pipeline);
    }

    /**
     * Удаляет этап из воронки. Если на нём есть лиды, их нужно перенести на другой этап (moveToStageId).
     * Этап архивируется, а не удаляется физически: на него ссылается история лидов.
     */
    @Transactional
    public PipelineView deleteStage(UUID pipelineId, UUID stageId, UUID moveToStageId) {
        Pipeline pipeline = requirePipeline(pipelineId);
        PipelineStage stage = requireStage(pipelineId, stageId);
        List<PipelineStage> active = activeStages(pipelineId);
        if (active.size() <= 1) {
            throw new IllegalArgumentException("В воронке должен остаться хотя бы один этап");
        }
        if (leadRepository.existsByStageId(stageId)) {
            if (moveToStageId == null) {
                throw new IllegalArgumentException("На этапе есть лиды: укажите этап для их переноса (moveToStageId)");
            }
            if (moveToStageId.equals(stageId)) {
                throw new IllegalArgumentException("Нельзя перенести лиды на удаляемый этап");
            }
            requireStage(pipelineId, moveToStageId);
            leadRepository.moveAllToStage(stageId, moveToStageId);
        }
        stage.setArchived(true);
        int position = 0;
        for (PipelineStage s : active) {
            if (!s.getId().equals(stageId)) {
                s.setPosition(position++);
            }
        }
        return view(pipeline);
    }

    // ---------- Причины проигрыша ----------

    @Transactional
    public PipelineView addLostReason(UUID pipelineId, String name) {
        Pipeline pipeline = requirePipeline(pipelineId);
        int position = lostReasonRepository.findAllByPipelineIdOrderByPositionAsc(pipelineId).size();
        lostReasonRepository.save(LostReason.builder()
                .pipelineId(pipelineId).name(name.trim()).position(position).active(true).build());
        return view(pipeline);
    }

    /** Причины не удаляются (на них ссылаются лиды) — неактуальную можно выключить. */
    @Transactional
    public PipelineView updateLostReason(UUID pipelineId, UUID reasonId, LostReasonUpdateRequest request) {
        LostReason reason = lostReasonRepository.findById(reasonId)
                .filter(r -> r.getPipelineId().equals(pipelineId))
                .orElseThrow(() -> new NotFoundException("Причина не найдена"));
        reason.setName(request.name().trim());
        reason.setActive(request.active());
        return view(requirePipeline(pipelineId));
    }

    // ---------- Для LeadService ----------

    Pipeline requirePipeline(UUID pipelineId) {
        return pipelineRepository.findById(pipelineId)
                .filter(p -> !p.isArchived())
                .orElseThrow(() -> new NotFoundException("Воронка не найдена"));
    }

    Pipeline defaultPipeline() {
        return pipelineRepository.findByDefaultPipelineTrue()
                .orElseGet(() -> createPipeline(DEFAULT_NAME, LostReasonMode.OPTIONAL, true,
                        DEFAULT_STAGES, DEFAULT_LOST_REASONS));
    }

    /** Этап должен принадлежать указанной воронке и не быть удалённым. */
    PipelineStage requireStage(UUID pipelineId, UUID stageId) {
        return stageRepository.findById(stageId)
                .filter(s -> s.getPipelineId().equals(pipelineId) && !s.isArchived())
                .orElseThrow(() -> new IllegalArgumentException("Этап не относится к воронке лида"));
    }

    PipelineStage firstStage(UUID pipelineId) {
        return activeStages(pipelineId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("В воронке нет этапов"));
    }

    /** Активная причина проигрыша этой воронки. */
    LostReason requireLostReason(UUID pipelineId, UUID reasonId) {
        return lostReasonRepository.findById(reasonId)
                .filter(r -> r.getPipelineId().equals(pipelineId) && r.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Причина проигрыша не найдена в этой воронке"));
    }

    // ---------- Внутреннее ----------

    private Pipeline createPipeline(String name, LostReasonMode mode, boolean isDefault,
                                    List<String> stageNames, List<String> reasonNames) {
        Pipeline pipeline = pipelineRepository.save(Pipeline.builder()
                .name(name).lostReasonMode(mode).defaultPipeline(isDefault).archived(false).build());
        for (int i = 0; i < stageNames.size(); i++) {
            stageRepository.save(PipelineStage.builder()
                    .pipelineId(pipeline.getId()).name(stageNames.get(i).trim()).position(i).build());
        }
        for (int i = 0; i < reasonNames.size(); i++) {
            lostReasonRepository.save(LostReason.builder()
                    .pipelineId(pipeline.getId()).name(reasonNames.get(i).trim()).position(i).active(true).build());
        }
        return pipeline;
    }

    private List<PipelineStage> activeStages(UUID pipelineId) {
        return stageRepository.findAllByPipelineIdAndArchivedFalseOrderByPositionAsc(pipelineId);
    }

    private PipelineView view(Pipeline pipeline) {
        stageRepository.flush(); // позиции/переименования видны в выборке ниже
        return PipelineView.of(pipeline, activeStages(pipeline.getId()),
                lostReasonRepository.findAllByPipelineIdOrderByPositionAsc(pipeline.getId()));
    }
}
