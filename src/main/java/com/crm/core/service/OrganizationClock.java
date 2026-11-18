package com.crm.core.service;

import com.crm.core.repository.OrganizationRepository;
import com.crm.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Текущее время в часовом поясе организации (сроки задач, «сегодня», просрочка). */
@Component
@RequiredArgsConstructor
public class OrganizationClock {

    private final OrganizationRepository organizationRepository;

    public LocalDateTime now() {
        String timezone = organizationRepository.findById(TenantContext.require())
                .map(org -> org.getTimezone())
                .orElse("UTC");
        return LocalDateTime.now(ZoneId.of(timezone));
    }
}
