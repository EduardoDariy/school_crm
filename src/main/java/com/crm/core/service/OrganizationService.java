package com.crm.core.service;

import com.crm.core.dto.OrganizationCreateDto;
import com.crm.core.entity.Organization;
import com.crm.core.entity.enums.OrganizationStatus;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    /** Допустимая метка DNS: строчные латинские буквы, цифры и дефис, 3–63 символа. */
    private static final Pattern SUBDOMAIN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");

    private final OrganizationRepository organizationRepository;

    @Transactional
    public Organization create(OrganizationCreateDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("Название организации обязательно");
        }
        String subdomain = dto.subdomain() == null ? "" : dto.subdomain().trim().toLowerCase(Locale.ROOT);
        if (!SUBDOMAIN.matcher(subdomain).matches()) {
            throw new IllegalArgumentException(
                    "Поддомен: 3–63 символа, латинские буквы, цифры и дефис, без дефиса по краям");
        }
        if (organizationRepository.existsBySubdomain(subdomain)) {
            throw new IllegalArgumentException("Поддомен «" + subdomain + "» уже занят");
        }

        Organization organization = Organization.builder()
                .name(dto.name().trim())
                .subdomain(subdomain)
                .build();
        if (dto.timezone() != null && !dto.timezone().isBlank()) {
            organization.setTimezone(validTimezone(dto.timezone().trim()));
        }
        if (dto.industryModule() != null) {
            organization.setIndustryModule(dto.industryModule());
        }
        return organizationRepository.save(organization);
    }

    public List<Organization> findAll() {
        return organizationRepository.findAll();
    }

    public Organization get(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Организация не найдена"));
    }

    @Transactional
    public Organization changeStatus(UUID id, OrganizationStatus status) {
        Organization organization = get(id);
        organization.setStatus(status);
        return organization;
    }

    private static String validTimezone(String timezone) {
        try {
            return ZoneId.of(timezone).getId();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Неизвестная временная зона: " + timezone);
        }
    }
}
