package com.crm.core.controller;

import com.crm.core.dto.auth.PlatformLoginRequest;
import com.crm.core.dto.auth.TokenResponse;
import com.crm.core.security.ClientInfo;
import com.crm.core.security.CurrentUser;
import com.crm.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Вход представителя CRM (суперадминистратора). */
@Tag(name = "Платформа: вход", description = "Вход представителя CRM")
@RestController
@RequestMapping("/api/platform/auth")
@RequiredArgsConstructor
public class PlatformAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements // токен не нужен
    public TokenResponse login(@Valid @RequestBody PlatformLoginRequest request, HttpServletRequest http) {
        return authService.platformLogin(request, ClientInfo.from(http));
    }

    @GetMapping("/me")
    public CurrentUser me() {
        return CurrentUser.get();
    }
}
