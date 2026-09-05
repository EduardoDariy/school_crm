package com.crm.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "group_id")
    private UUID groupId; // Если null, абонемент действует на любые группы

    @Column(name = "lessons_total", nullable = false)
    private Integer lessonsTotal;

    @Column(name = "lessons_remaining", nullable = false)
    private Integer lessonsRemaining;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}