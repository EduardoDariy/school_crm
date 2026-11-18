package com.crm.core.controller;

import com.crm.core.dto.OrganizationCreateDto;
import com.crm.core.entity.Organization;
import com.crm.core.entity.enums.OrganizationStatus;
import com.crm.core.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Platform API представителя CRM: создание и управление организациями.
 * TODO(auth): доступ только для роли суперадминистратора — добавляется на шаге аутентификации.
 */
@RestController
@RequestMapping("/api/platform/organizations")
@RequiredArgsConstructor
public class PlatformOrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody OrganizationCreateDto dto) {
        return organizationService.create(dto);
    }

    @GetMapping
    public List<Organization> list() {
        return organizationService.findAll();
    }

    @GetMapping("/{id}")
    public Organization get(@PathVariable UUID id) {
        return organizationService.get(id);
    }

    @PostMapping("/{id}/block")
    public Organization block(@PathVariable UUID id) {
        return organizationService.changeStatus(id, OrganizationStatus.BLOCKED);
    }

    @PostMapping("/{id}/activate")
    public Organization activate(@PathVariable UUID id) {
        return organizationService.changeStatus(id, OrganizationStatus.ACTIVE);
    }
}
