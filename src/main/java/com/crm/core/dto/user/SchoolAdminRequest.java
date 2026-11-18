package com.crm.core.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Первый администратор школы, которого заводит представитель CRM при подключении организации. */
public record SchoolAdminRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank String password
) {}
