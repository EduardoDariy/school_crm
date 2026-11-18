package com.crm.core.repository;

import com.crm.core.entity.LoginAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** LoginAttempt не помечен @TenantId — организация всегда передаётся явно. */
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    Page<LoginAttempt> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<LoginAttempt> findAllByPlatformAdminTrueOrderByCreatedAtDesc(Pageable pageable);
}
