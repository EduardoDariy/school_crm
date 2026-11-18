package com.crm.core.repository;

import com.crm.core.entity.LostReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LostReasonRepository extends JpaRepository<LostReason, UUID> {

    List<LostReason> findAllByPipelineIdOrderByPositionAsc(UUID pipelineId);

    List<LostReason> findAllByPipelineIdInOrderByPositionAsc(Collection<UUID> pipelineIds);
}
