package com.crm.core.entity;

import com.crm.core.entity.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(name = "grade")
    private Integer grade; // Оценка за урок (например, от 1 до 5 или до 100)

    @Column(name = "note")
    private String note; // Комментарий преподавателя
}