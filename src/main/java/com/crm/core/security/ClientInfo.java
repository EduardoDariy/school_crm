package com.crm.core.security;

import jakarta.servlet.http.HttpServletRequest;

/** Сведения о клиенте для журнала входов. */
public record ClientInfo(String ipAddress, String userAgent) {

    private static final int MAX_USER_AGENT = 512;

    /**
     * IP берётся из {@code getRemoteAddr()}: за nginx Tomcat подставляет адрес из
     * X-Forwarded-For только от доверенного внутреннего прокси (server.forward-headers-strategy).
     */
    public static ClientInfo from(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > MAX_USER_AGENT) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT);
        }
        return new ClientInfo(request.getRemoteAddr(), userAgent);
    }
}
