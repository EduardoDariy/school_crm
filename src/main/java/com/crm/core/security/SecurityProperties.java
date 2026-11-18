package com.crm.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param jwtSecret      ключ подписи JWT (HS256), не короче 32 байт. Если пуст — генерируется
 *                       случайный при старте (токены перестают действовать после перезапуска).
 * @param tokenTtl       срок действия токена доступа
 * @param bootstrapAdmin первый суперадминистратор; создаётся, только если их ещё нет
 */
@ConfigurationProperties("crm.security")
public record SecurityProperties(String jwtSecret, Duration tokenTtl, BootstrapAdmin bootstrapAdmin) {

    public SecurityProperties {
        if (tokenTtl == null) {
            tokenTtl = Duration.ofHours(12);
        }
        if (bootstrapAdmin == null) {
            bootstrapAdmin = new BootstrapAdmin(null, null, null);
        }
    }

    public record BootstrapAdmin(String email, String password, String fullName) {
    }
}
