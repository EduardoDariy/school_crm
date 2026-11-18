package com.crm.core.exception;

import lombok.Getter;

import java.util.UUID;

/** Запись с таким уникальным идентификатором уже есть; existingId — её id для перехода к карточке. */
@Getter
public class DuplicateException extends RuntimeException {

    private final UUID existingId;

    public DuplicateException(String message, UUID existingId) {
        super(message);
        this.existingId = existingId;
    }
}
