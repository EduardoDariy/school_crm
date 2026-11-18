package com.crm.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Неуспешный вход. Сообщение намеренно общее, чтобы не раскрывать, существует ли логин. */
@Getter
public class LoginFailedException extends RuntimeException {

    private final HttpStatus status;

    public LoginFailedException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
