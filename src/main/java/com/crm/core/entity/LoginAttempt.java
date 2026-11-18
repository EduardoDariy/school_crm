package com.crm.core.entity;

import com.crm.core.entity.enums.LoginFailureReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Запись журнала входов. Не наследует {@link BaseEntity}: попытка может не относиться
 * ни к одной организации (platform API, неизвестный поддомен), поэтому tenant фильтруется явно.
 */
@Entity
@Immutable
@Table(name = "login_attempts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "platform_admin", nullable = false)
    private boolean platformAdmin;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    private LoginFailureReason failureReason;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
