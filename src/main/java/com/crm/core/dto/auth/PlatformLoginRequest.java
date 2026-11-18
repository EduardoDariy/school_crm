package com.crm.core.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record PlatformLoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
