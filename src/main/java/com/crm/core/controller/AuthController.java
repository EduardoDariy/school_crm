package com.crm.core.controller;

import com.crm.core.dto.auth.ChangePasswordRequest;
import com.crm.core.dto.auth.LoginRequest;
import com.crm.core.dto.auth.TokenResponse;
import com.crm.core.security.ClientInfo;
import com.crm.core.security.CurrentUser;
import com.crm.core.service.AuthService;
import com.crm.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Вход сотрудников школы. */
@Tag(name = "Вход", description = "Вход сотрудника школы, текущий пользователь, смена пароля")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @SecurityRequirements // токен не нужен
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, ClientInfo.from(http));
    }

    @GetMapping("/me")
    public CurrentUser me() {
        return CurrentUser.get();
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(request.currentPassword(), request.newPassword());
    }
}
