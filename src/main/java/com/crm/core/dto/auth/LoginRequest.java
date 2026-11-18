package com.crm.core.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Вход сотрудника: поддомен школы определяет организацию (брендированное окно входа). */
public record LoginRequest(
        @NotBlank String subdomain,
        @NotBlank String email,
        @NotBlank String password
) {}
