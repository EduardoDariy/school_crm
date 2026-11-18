package com.crm.core.security;

import com.crm.core.entity.Organization;
import com.crm.core.entity.PlatformAdmin;
import com.crm.core.entity.User;
import com.crm.core.entity.enums.OrganizationStatus;
import com.crm.core.repository.OrganizationRepository;
import com.crm.core.repository.PlatformAdminRepository;
import com.crm.core.repository.UserRepository;
import com.crm.core.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Стоит сразу после проверки подписи JWT. По идентификаторам из токена загружает
 * сотрудника и его организацию, проверяет блокировки и устанавливает:
 * <ul>
 *     <li>{@link TenantContext} — организацию, по которой Hibernate фильтрует данные;</li>
 *     <li>{@link CrmAuthentication} — актуальную роль из БД (а не из токена).</li>
 * </ul>
 * Поэтому блокировка сотрудника или школы и смена роли действуют сразу, без ожидания истечения токена.
 * Не регистрируется как отдельный servlet-фильтр — только внутри цепочки Spring Security.
 */
@RequiredArgsConstructor
public class AuthContextFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            chain.doFilter(request, response); // анонимный запрос: решает AuthorizationFilter
            return;
        }

        Jwt jwt = jwtAuth.getToken();
        Optional<UUID> subject = parseUuid(jwt.getSubject());
        if (subject.isEmpty()) {
            ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Недействительный токен");
            return;
        }

        String type = jwt.getClaimAsString(JwtService.CLAIM_TYPE);
        if (JwtService.TYPE_PLATFORM.equals(type)) {
            Optional<PlatformAdmin> admin = platformAdminRepository.findById(subject.get());
            if (admin.isEmpty() || admin.get().isBlocked()) {
                ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Учётная запись недоступна");
                return;
            }
            authenticate(CurrentUser.of(admin.get()));
            chain.doFilter(request, response);
            return;
        }

        if (!JwtService.TYPE_STAFF.equals(type)) {
            ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Недействительный токен");
            return;
        }

        Optional<Organization> organization = parseUuid(jwt.getClaimAsString(JwtService.CLAIM_TENANT))
                .flatMap(organizationRepository::findById);
        if (organization.isEmpty()) {
            ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Недействительный токен");
            return;
        }
        if (organization.get().getStatus() == OrganizationStatus.BLOCKED) {
            ProblemResponses.write(response, HttpStatus.FORBIDDEN, "Организация заблокирована");
            return;
        }

        TenantContext.set(organization.get().getId());
        try {
            // Поиск уже ограничен организацией из токена
            Optional<User> user = userRepository.findById(subject.get());
            if (user.isEmpty() || user.get().isBlocked()) {
                ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Учётная запись недоступна");
                return;
            }
            authenticate(CurrentUser.of(user.get()));
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static void authenticate(CurrentUser user) {
        SecurityContextHolder.getContext().setAuthentication(new CrmAuthentication(user));
    }

    private static Optional<UUID> parseUuid(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
