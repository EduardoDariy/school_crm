package com.crm.core.security;

import com.crm.core.entity.PlatformAdmin;
import com.crm.core.repository.PlatformAdminRepository;
import com.crm.core.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Создаёт первого суперадминистратора из настроек (CRM_ADMIN_EMAIL / CRM_ADMIN_PASSWORD),
 * если в системе ещё нет ни одного. Существующие учётные записи не изменяет.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminBootstrap implements ApplicationRunner {

    private final SecurityProperties properties;
    private final PlatformAdminRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        SecurityProperties.BootstrapAdmin admin = properties.bootstrapAdmin();
        if (admin.email() == null || admin.email().isBlank() || admin.password() == null || admin.password().isBlank()) {
            log.warn("Нет ни одного суперадминистратора. Задайте CRM_ADMIN_EMAIL и CRM_ADMIN_PASSWORD, "
                    + "чтобы создать его при запуске");
            return;
        }
        PasswordPolicy.validate(admin.password());

        String email = AuthService.normalizeEmail(admin.email());
        repository.save(PlatformAdmin.builder()
                .email(email)
                .fullName(admin.fullName() == null || admin.fullName().isBlank() ? "Администратор CRM" : admin.fullName())
                .passwordHash(passwordEncoder.encode(admin.password()))
                .blocked(false)
                .build());
        log.info("Создан суперадминистратор {}", email);
    }
}
