package com.crm.core.controller;

import com.crm.core.entity.Organization;
import com.crm.core.service.OrganizationService;
import com.crm.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Сведения о текущей организации (название, модуль, временная зона) для интерфейса школы. */
@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public Organization current() {
        return organizationService.get(TenantContext.require());
    }
}
