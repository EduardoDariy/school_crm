package com.crm.core.dto.user;

import com.crm.core.entity.User;
import com.crm.core.entity.enums.StaffRole;

import java.time.LocalDateTime;
import java.util.UUID;

/** Сотрудник в ответах API — без хеша пароля и служебных полей. */
public record UserView(
        UUID id,
        String email,
        String fullName,
        StaffRole role,
        boolean blocked,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static UserView of(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isBlocked(), user.getLastLoginAt(), user.getCreatedAt());
    }
}
