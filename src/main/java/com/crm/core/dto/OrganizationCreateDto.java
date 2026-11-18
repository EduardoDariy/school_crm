package com.crm.core.dto;

import com.crm.core.entity.enums.IndustryModule;

/** Создание организации представителем CRM. timezone и industryModule необязательны. */
public record OrganizationCreateDto(
        String name,
        String subdomain,
        String timezone,
        IndustryModule industryModule
) {}
