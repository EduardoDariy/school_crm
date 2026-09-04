package com.crm.core.repository;

import com.crm.core.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    // Spring Data автоматически сгенерирует SQL-запросы по имени метода.
    // Находим все неархивированные лиды конкретной организации (tenantId)
    List<Lead> findAllByTenantIdAndIsArchivedFalse(UUID tenantId);

    // Проверка дублей по телефону внутри одной организации
    boolean existsByTenantIdAndPhoneNumber(UUID tenantId, String phoneNumber);
}