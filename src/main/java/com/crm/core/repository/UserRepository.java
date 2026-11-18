package com.crm.core.repository;

import com.crm.core.entity.User;
import com.crm.core.entity.enums.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Все запросы ограничены текущей организацией (@TenantId). */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByFullNameAsc();

    long countByRoleAndBlockedFalse(StaffRole role);
}
