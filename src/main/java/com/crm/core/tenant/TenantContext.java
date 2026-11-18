package com.crm.core.tenant;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Текущая организация (tenant) для потока, обрабатывающего запрос.
 * Заполняется {@code AuthContextFilter} по токену сотрудника, читается Hibernate
 * через {@link TenantIdentifierResolver}.
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

    /**
     * Выполняет действие от имени организации (вход по поддомену, действия представителя CRM).
     * Сессия Hibernate фиксирует организацию при открытии, поэтому вызывать только вне
     * уже начатой транзакции (open-in-view выключен).
     */
    public static <T> T callAs(UUID tenantId, Supplier<T> action) {
        UUID previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
