package com.crm.core.entity;

import com.crm.core.entity.enums.IndustryModule;
import com.crm.core.entity.enums.OrganizationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Организация (школа) — единица изоляции данных в SaaS.
 * Сама не принадлежит какому-либо tenant, поэтому не наследует {@link BaseEntity}.
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Поддомен для брендированного входа; назначается при создании и не меняется школой. */
    @Column(nullable = false, unique = true, updatable = false)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    /** Одна временная зона на организацию (расписание, задачи). */
    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Asia/Almaty";

    /** В MVP у организации один активный отраслевой модуль. */
    @Enumerated(EnumType.STRING)
    @Column(name = "industry_module", nullable = false)
    @Builder.Default
    private IndustryModule industryModule = IndustryModule.LANGUAGE_SCHOOL;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
