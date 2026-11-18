package com.crm.core.security;

import com.crm.core.entity.enums.StaffRole;
import com.crm.core.repository.OrganizationRepository;
import com.crm.core.repository.PlatformAdminRepository;
import com.crm.core.repository.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            UserRepository userRepository,
                                            PlatformAdminRepository platformAdminRepository,
                                            OrganizationRepository organizationRepository) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, e) ->
                ProblemResponses.write(response, HttpStatus.UNAUTHORIZED, "Требуется вход в систему");
        AccessDeniedHandler forbidden = (request, response, e) ->
                ProblemResponses.write(response, HttpStatus.FORBIDDEN, "Недостаточно прав");

        String[] staffRoles = Arrays.stream(StaffRole.values()).map(Enum::name).toArray(String[]::new);

        http
                // Stateless API с Bearer-токеном: cookie-сессий нет, CSRF не применим
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> {}).authenticationEntryPoint(unauthorized))
                .addFilterAfter(new AuthContextFilter(userRepository, platformAdminRepository, organizationRepository),
                        BearerTokenAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/platform/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/platform/**").hasRole(CurrentUser.PLATFORM_ADMIN_ROLE)
                        // Права по ролям внутри школы — через @PreAuthorize на контроллерах
                        .requestMatchers("/api/**").hasAnyRole(staffRoles)
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // bcrypt по умолчанию, с префиксом алгоритма для будущей миграции хешей
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
