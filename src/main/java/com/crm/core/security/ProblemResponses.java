package com.crm.core.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Ответ об ошибке в формате Problem Details из фильтров (до контроллеров). */
final class ProblemResponses {

    private ProblemResponses() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // detail — только фиксированные строки из кода, экранирование не требуется
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"" + detail + "\"}");
    }
}
