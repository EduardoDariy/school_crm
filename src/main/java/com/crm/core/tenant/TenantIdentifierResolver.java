package com.crm.core.tenant;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Сообщает Hibernate текущую организацию. Благодаря {@code @TenantId} в {@code BaseEntity}
 * Hibernate сам добавляет условие {@code tenant_id = ?} во все запросы (включая findById)
 * и проставляет tenant_id при сохранении — ручные проверки принадлежности не нужны.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    /**
     * Используется, когда организация не определена (например, platform-эндпоинты).
     * Такой организации не существует: чтение данных школ вернёт пусто, запись упадёт на FK.
     */
    static final UUID NO_TENANT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.get().orElse(NO_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
