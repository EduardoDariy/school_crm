package com.crm.core.repository;

import com.crm.core.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.crm.core.dto.DebtorReportDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // Получаем самый старый активный абонемент для списания по FIFO
    Optional<Subscription> findFirstByTenantIdAndStudentIdAndIsActiveTrueOrderByCreatedAtAsc(
            UUID tenantId, UUID studentId);

    List<Subscription> findAllByTenantIdAndStudentIdAndIsActiveTrue(UUID tenantId, UUID studentId);

    @Query("SELECT new com.crm.core.dto.DebtorReportDto(p.id, p.firstName, p.lastName, p.phoneNumber, s.lessonsRemaining) " +
            "FROM Subscription s JOIN Person p ON s.studentId = p.id " +
            "WHERE s.tenantId = :tenantId AND s.lessonsRemaining <= 0 AND p.isStudent = true")
    List<DebtorReportDto> findDebtorsByTenantId(@Param("tenantId") UUID tenantId);

}