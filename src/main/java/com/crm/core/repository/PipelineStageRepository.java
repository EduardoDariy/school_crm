package com.crm.core.repository;

import com.crm.core.entity.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {

    List<PipelineStage> findAllByPipelineIdAndArchivedFalseOrderByPositionAsc(UUID pipelineId);

    List<PipelineStage> findAllByPipelineIdInAndArchivedFalseOrderByPositionAsc(Collection<UUID> pipelineIds);
}
