package com.crm.core.repository;

import com.crm.core.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsBySubdomain(String subdomain);

    Optional<Organization> findBySubdomain(String subdomain);
}
