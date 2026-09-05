package com.crm.core.repository;

import com.crm.core.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // Получаем самый старый активный абонемент для списания по FIFO
    Optional<Subscription> findFirstByTenantIdAndStudentIdAndIsActiveTrueOrderByCreatedAtAsc(
            UUID tenantId, UUID studentId);

    List<Subscription> findAllByTenantIdAndStudentIdAndIsActiveTrue(UUID tenantId, UUID studentId);
}