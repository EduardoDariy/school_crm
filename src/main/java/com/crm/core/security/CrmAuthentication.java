package com.crm.core.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/** Аутентификация с актуальными данными пользователя из БД (роль, организация). */
public class CrmAuthentication extends AbstractAuthenticationToken {

    private final CurrentUser user;

    public CrmAuthentication(CurrentUser user) {
        super(List.of(new SimpleGrantedAuthority(user.authority())));
        this.user = user;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public CurrentUser getPrincipal() {
        return user;
    }

    @Override
    public String getName() {
        return user.id().toString();
    }
}
