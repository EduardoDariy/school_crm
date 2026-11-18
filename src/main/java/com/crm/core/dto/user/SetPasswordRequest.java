package com.crm.core.dto.user;

import jakarta.validation.constraints.NotBlank;

/** Новый пароль, который администратор школы задаёт сотруднику. */
public record SetPasswordRequest(@NotBlank String password) {
}
