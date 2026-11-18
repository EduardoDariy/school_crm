package com.crm.core.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Выпуск и проверка JWT. В токене только идентификаторы (sub, tid, typ):
 * роль и статус блокировки проверяются по БД при каждом запросе.
 */
@Slf4j
@Component
public class JwtService {

    static final String CLAIM_TYPE = "typ";
    static final String CLAIM_TENANT = "tid";
    static final String TYPE_STAFF = "staff";
    static final String TYPE_PLATFORM = "platform";
    private static final String ISSUER = "school-crm";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecurityProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        SecretKey key = new SecretKeySpec(secretBytes(properties.jwtSecret()), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        nimbusDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        this.decoder = nimbusDecoder;
    }

    public JwtDecoder decoder() {
        return decoder;
    }

    public IssuedToken issue(CurrentUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.tokenTtl());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CLAIM_TYPE, user.isPlatformAdmin() ? TYPE_PLATFORM : TYPE_STAFF);
        if (!user.isPlatformAdmin()) {
            claims.claim(CLAIM_TENANT, user.tenantId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    private static byte[] secretBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("crm.security.jwt-secret (CRM_JWT_SECRET) не задан: используется случайный ключ, "
                    + "токены станут недействительны после перезапуска");
            byte[] random = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("crm.security.jwt-secret должен быть не короче " + MIN_SECRET_BYTES + " байт");
        }
        return bytes;
    }
}
