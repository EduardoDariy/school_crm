package com.crm.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "messenger_id")
    private String messengerId;

    @Column(name = "source")
    private String source;

    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "is_archived")
    private boolean isArchived = false;
}