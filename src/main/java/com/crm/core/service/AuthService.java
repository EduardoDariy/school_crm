package com.crm.core.service;

import com.crm.core.dto.auth.LoginRequest;
import com.crm.core.dto.auth.PlatformLoginRequest;
import com.crm.core.dto.auth.TokenResponse;
import com.crm.core.entity.Organization;
import com.crm.core.entity.PlatformAdmin;
import com.crm.core.entity.User;
import com.crm.core.entity.enums.LoginFailureReason;
import com.crm.core.entity.enums.OrganizationStatus;
import com.crm.core.exception.LoginFailedException;
import com.crm.core.repository.OrganizationRepository;
import com.crm.core.repository.PlatformAdminRepository;
import com.crm.core.repository.UserRepository;
import com.crm.core.security.ClientInfo;
import com.crm.core.security.CurrentUser;
import com.crm.core.security.JwtService;
import com.crm.core.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Вход по логину и паролю. Намеренно без @Transactional: запись в журнал входов
 * должна сохраниться и при неуспешной попытке.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Неверный email или пароль";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginJournalService journal;
    /** Проверка пароля против фиктивного хеша выравнивает время ответа для несуществующих логинов. */
    private final String dummyHash;

    public AuthService(OrganizationRepository organizationRepository, UserRepository userRepository,
                       PlatformAdminRepository platformAdminRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, LoginJournalService journal) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.journal = journal;
        this.dummyHash = passwordEncoder.encode("timing-equalizer");
    }

    public TokenResponse login(LoginRequest request, ClientInfo client) {
        String email = normalizeEmail(request.email());
        String subdomain = request.subdomain().trim().toLowerCase(Locale.ROOT);

        Optional<Organization> organization = organizationRepository.findBySubdomain(subdomain);
        if (organization.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            journal.staffFailure(null, null, email, LoginFailureReason.UNKNOWN_ORGANIZATION, client);
            throw new LoginFailedException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        Organization org = organization.get();
        if (org.getStatus() == OrganizationStatus.BLOCKED) {
            journal.staffFailure(org.getId(), null, email, LoginFailureReason.ORGANIZATION_BLOCKED, client);
            throw new LoginFailedException(HttpStatus.FORBIDDEN, "Организация заблокирована");
        }

        return TenantContext.callAs(org.getId(), () -> loginStaff(org, email, request.password(), client));
    }

    private TokenResponse loginStaff(Organization org, String email, String password, ClientInfo client) {
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            journal.staffFailure(org.getId(), null, email, LoginFailureReason.UNKNOWN_USER, client);
            throw new LoginFailedException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            journal.staffFailure(org.getId(), user.getId(), email, LoginFailureReason.BAD_PASSWORD, client);
            throw new LoginFailedException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        // О блокировке сообщаем только после верного пароля, чтобы не раскрывать статус учётной записи
        if (user.isBlocked()) {
            journal.staffFailure(org.getId(), user.getId(), email, LoginFailureReason.USER_BLOCKED, client);
            throw new LoginFailedException(HttpStatus.FORBIDDEN, "Учётная запись заблокирована");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        journal.staffSuccess(org.getId(), user.getId(), email, client);

        CurrentUser current = CurrentUser.of(user);
        JwtService.IssuedToken token = jwtService.issue(current);
        return new TokenResponse(token.value(), token.expiresAt(), current);
    }

    public TokenResponse platformLogin(PlatformLoginRequest request, ClientInfo client) {
        String email = normalizeEmail(request.email());
        Optional<PlatformAdmin> found = platformAdminRepository.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            journal.platformAttempt(email, LoginFailureReason.UNKNOWN_USER, client);
            throw new LoginFailedException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        PlatformAdmin admin = found.get();
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            journal.platformAttempt(email, LoginFailureReason.BAD_PASSWORD, client);
            throw new LoginFailedException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (admin.isBlocked()) {
            journal.platformAttempt(email, LoginFailureReason.USER_BLOCKED, client);
            throw new LoginFailedException(HttpStatus.FORBIDDEN, "Учётная запись заблокирована");
        }
        journal.platformAttempt(email, null, client);

        CurrentUser current = CurrentUser.of(admin);
        JwtService.IssuedToken token = jwtService.issue(current);
        return new TokenResponse(token.value(), token.expiresAt(), current);
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
