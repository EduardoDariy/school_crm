package com.crm.core.dto.user;

import com.crm.core.entity.enums.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotNull StaffRole role,
        @NotBlank String password
) {}
