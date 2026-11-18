package com.crm.core.security;

import com.crm.core.entity.PlatformAdmin;
import com.crm.core.entity.User;
import com.crm.core.entity.enums.StaffRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Аутентифицированный пользователь текущего запроса.
 *
 * @param tenantId организация сотрудника; {@code null} для представителя CRM
 * @param role     роль сотрудника; {@code null} для представителя CRM
 */
public record CurrentUser(UUID id, UUID tenantId, String email, String fullName, StaffRole role) {

    public static final String PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";

    public static CurrentUser of(User user) {
        return new CurrentUser(user.getId(), user.getTenantId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public static CurrentUser of(PlatformAdmin admin) {
        return new CurrentUser(admin.getId(), null, admin.getEmail(), admin.getFullName(), null);
    }

    public boolean isPlatformAdmin() {
        return tenantId == null;
    }

    public boolean hasRole(StaffRole staffRole) {
        return role == staffRole;
    }

    public String authority() {
        return isPlatformAdmin() ? "ROLE_" + PLATFORM_ADMIN_ROLE : role.authority();
    }

    /** Текущий пользователь; вызывается только из защищённых эндпоинтов. */
    public static CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser user) {
            return user;
        }
        throw new IllegalStateException("Пользователь не аутентифицирован");
    }
}
