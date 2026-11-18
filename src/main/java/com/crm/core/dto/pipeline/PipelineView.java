package com.crm.core.dto.pipeline;

import com.crm.core.entity.LostReason;
import com.crm.core.entity.Pipeline;
import com.crm.core.entity.PipelineStage;
import com.crm.core.entity.enums.LostReasonMode;

import java.util.List;
import java.util.UUID;

/** Воронка с этапами (по порядку) и причинами проигрыша. */
public record PipelineView(
        UUID id,
        String name,
        boolean defaultPipeline,
        LostReasonMode lostReasonMode,
        List<Stage> stages,
        List<Reason> lostReasons
) {
    public record Stage(UUID id, String name, int position) {
    }

    public record Reason(UUID id, String name, boolean active) {
    }

    public static PipelineView of(Pipeline pipeline, List<PipelineStage> stages, List<LostReason> reasons) {
        return new PipelineView(pipeline.getId(), pipeline.getName(), pipeline.isDefaultPipeline(),
                pipeline.getLostReasonMode(),
                stages.stream().map(s -> new Stage(s.getId(), s.getName(), s.getPosition())).toList(),
                reasons.stream().map(r -> new Reason(r.getId(), r.getName(), r.isActive())).toList());
    }
}
