package com.crm.core.repository;

import com.crm.core.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {

    // Блокировка при точном совпадении номера телефона внутри организации
    boolean existsByTenantIdAndPhoneNumber(UUID tenantId, String phoneNumber);

    // Глобальный поиск по имени или телефону
    List<Person> findAllByTenantIdAndFirstNameContainingIgnoreCaseOrPhoneNumberContaining(
            UUID tenantId, String firstName, String phoneNumber);
}