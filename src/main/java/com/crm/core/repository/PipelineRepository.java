package com.crm.core.repository;

import com.crm.core.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {

    Optional<Pipeline> findByDefaultPipelineTrue();

    List<Pipeline> findAllByArchivedFalseOrderByCreatedAtAsc();
}
