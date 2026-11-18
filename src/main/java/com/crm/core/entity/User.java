package com.crm.core.entity;

import com.crm.core.entity.enums.StaffRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Учётная запись сотрудника школы. В MVP принадлежит ровно одной организации. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    /** Логин; хранится в нижнем регистре, уникален в пределах организации. */
    @Column(nullable = false)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffRole role;

    /** Блокировка без удаления истории действий, платежей и назначений. */
    @Column(name = "is_blocked", nullable = false)
    private boolean blocked;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
