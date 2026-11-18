package com.crm.core.controller;

import com.crm.core.dto.OrganizationCreateDto;
import com.crm.core.dto.user.CreateUserRequest;
import com.crm.core.dto.user.SchoolAdminRequest;
import com.crm.core.dto.user.UserView;
import com.crm.core.entity.Organization;
import com.crm.core.entity.enums.OrganizationStatus;
import com.crm.core.entity.enums.StaffRole;
import com.crm.core.service.OrganizationService;
import com.crm.core.service.PipelineService;
import com.crm.core.service.UserService;
import com.crm.core.tenant.TenantContext;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Platform API представителя CRM: создание и управление организациями.
 * Доступ только для суперадминистратора (см. SecurityConfig). Данные школ здесь не читаются.
 */
@Tag(name = "Платформа: организации", description = "Подключение и блокировка школ")
@RestController
@RequestMapping("/api/platform/organizations")
@RequiredArgsConstructor
public class PlatformOrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;
    private final PipelineService pipelineService;

    /** Создаёт администратора школы — дальше он сам заводит сотрудников. */
    @PostMapping("/{id}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView createSchoolAdmin(@PathVariable UUID id, @Valid @RequestBody SchoolAdminRequest request) {
        organizationService.get(id); // 404, если организации нет
        return TenantContext.callAs(id, () -> userService.create(
                new CreateUserRequest(request.email(), request.fullName(), StaffRole.ADMIN, request.password())));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody OrganizationCreateDto dto) {
        Organization organization = organizationService.create(dto);
        // Стартовая воронка продаж — отдельной транзакцией уже в контексте новой школы
        TenantContext.callAs(organization.getId(), pipelineService::createDefault);
        return organization;
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
