package com.crm.core.controller;

import com.crm.core.entity.enums.StaffRole;
import com.crm.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Справочник сотрудников для выбора ответственного/исполнителя. В отличие от /api/users
 * доступен менеджеру и содержит только имя и роль.
 */
@Tag(name = "Сотрудники", description = "Управление сотрудниками и журнал входов (администратор школы)")
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class StaffController {

    private final UserService userService;

    public record StaffItem(UUID id, String fullName, StaffRole role, boolean blocked) {
    }

    @Operation(summary = "Справочник сотрудников (имя и роль) — для выбора ответственного")
    @GetMapping
    public List<StaffItem> directory() {
        return userService.list().stream()
                .map(user -> new StaffItem(user.id(), user.fullName(), user.role(), user.blocked()))
                .toList();
    }
}
