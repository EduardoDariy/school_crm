package com.crm.core.repository;

import com.crm.core.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findAllByTenantIdAndIsActiveTrue(UUID tenantId);
}