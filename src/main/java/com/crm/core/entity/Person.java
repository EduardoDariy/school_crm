package com.crm.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "messenger_id")
    private String messengerId;

    // Роли человека в системе согласно единой модели
    @Column(name = "is_student")
    private boolean isStudent = false;

    @Column(name = "is_payer")
    private boolean isPayer = false;

    @Column(name = "is_archived")
    private boolean isArchived = false;
}