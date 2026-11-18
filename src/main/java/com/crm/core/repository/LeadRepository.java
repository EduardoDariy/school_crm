package com.crm.core.repository;

import com.crm.core.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    // Spring Data автоматически сгенерирует SQL-запросы по имени метода.
    // Фильтр по организации добавляет Hibernate (@TenantId), здесь его указывать не нужно
    List<Lead> findAllByIsArchivedFalse();

    // Проверка дублей по телефону внутри одной организации
    boolean existsByPhoneNumber(String phoneNumber);
}