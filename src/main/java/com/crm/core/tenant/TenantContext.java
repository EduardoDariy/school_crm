package com.crm.core.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Текущая организация (tenant) для потока, обрабатывающего запрос.
 * Заполняется {@link TenantFilter}, читается Hibernate через {@link TenantIdentifierResolver}.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID require() {
        return get().orElseThrow(() -> new IllegalStateException("Организация не определена для текущего запроса"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
