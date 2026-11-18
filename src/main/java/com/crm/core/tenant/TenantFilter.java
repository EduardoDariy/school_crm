package com.crm.core.tenant;

import com.crm.core.entity.Organization;
import com.crm.core.entity.enums.OrganizationStatus;
import com.crm.core.repository.OrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Определяет организацию для каждого запроса к API школы.
 * <p>
 * Временно организация берётся из заголовка {@value #TENANT_HEADER}. После внедрения
 * аутентификации она будет определяться по учётной записи сотрудника, а заголовок уйдёт.
 */
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final OrganizationRepository organizationRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // Platform API (представитель CRM) работает вне конкретной организации
        return !path.startsWith("/api/") || path.startsWith("/api/platform/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(TENANT_HEADER);
        if (header == null || header.isBlank()) {
            reject(response, HttpStatus.BAD_REQUEST, "Не указана организация (заголовок " + TENANT_HEADER + ")");
            return;
        }

        Optional<Organization> organization = parseUuid(header).flatMap(organizationRepository::findById);
        if (organization.isEmpty()) {
            reject(response, HttpStatus.NOT_FOUND, "Организация не найдена");
            return;
        }
        if (organization.get().getStatus() == OrganizationStatus.BLOCKED) {
            reject(response, HttpStatus.FORBIDDEN, "Организация заблокирована");
            return;
        }

        TenantContext.set(organization.get().getId());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"" + detail + "\"}");
    }
}
