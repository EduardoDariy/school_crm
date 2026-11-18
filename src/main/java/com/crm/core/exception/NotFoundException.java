package com.crm.core.exception;

/** Запрошенная запись не существует в текущей организации. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
