package com.crm.core.repository;

import com.crm.core.entity.LeadEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeadEventRepository extends JpaRepository<LeadEvent, UUID> {

    List<LeadEvent> findAllByLeadIdOrderByCreatedAtAsc(UUID leadId);
}
