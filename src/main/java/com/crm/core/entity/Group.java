package com.crm.core.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groups") // 'group' — зарезервированное слово в SQL, используем 'groups'
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "is_active")
    private boolean isActive = true;

    @ManyToMany
    @JoinTable(
            name = "student_groups",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Person> students = new HashSet<>();
}