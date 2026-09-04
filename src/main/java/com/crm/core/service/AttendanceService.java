package com.crm.core.service;

import com.crm.core.dto.AttendanceDto;
import com.crm.core.entity.Attendance;
import com.crm.core.entity.Lesson;
import com.crm.core.repository.AttendanceRepository;
import com.crm.core.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public List<Attendance> saveBulkAttendance(UUID tenantId, UUID lessonId, List<AttendanceDto> dtos) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));

        if (!lesson.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Ошибка доступа: урок принадлежит другой организации");
        }

        // Удаляем старые записи посещаемости для этого урока, если они были, чтобы перезаписать актуальными
        List<Attendance> existing = attendanceRepository.findAllByLessonId(lessonId);
        attendanceRepository.deleteAll(existing);

        List<Attendance> listToSave = dtos.stream().map(dto -> {
            Attendance attendance = Attendance.builder()
                    .lessonId(lessonId)
                    .studentId(dto.getStudentId())
                    .status(dto.getStatus())
                    .grade(dto.getGrade())
                    .note(dto.getNote())
                    .build();
            attendance.setTenantId(tenantId);
            return attendance;
        }).collect(Collectors.toList());

        return attendanceRepository.saveAll(listToSave);
    }
}