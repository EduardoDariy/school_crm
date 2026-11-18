package com.crm.core.dto.auth;

import com.crm.core.security.CurrentUser;

import java.time.Instant;

public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, CurrentUser user) {

    public TokenResponse(String accessToken, Instant expiresAt, CurrentUser user) {
        this(accessToken, "Bearer", expiresAt, user);
    }
}
