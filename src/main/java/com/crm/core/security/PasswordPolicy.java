package com.crm.core.security;

import java.nio.charset.StandardCharsets;

/** Минимальные требования к паролю сотрудника (MVP). */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** bcrypt учитывает только первые 72 байта — длиннее не принимаем, чтобы не вводить в заблуждение. */
    private static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Пароль должен быть не короче " + MIN_LENGTH + " символов");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("Пароль слишком длинный");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("Пароль не может состоять из пробелов");
        }
    }
}
