package com.crm.core.service;

import com.crm.core.dto.AttendanceDto;
import com.crm.core.entity.Attendance;
import com.crm.core.entity.enums.AttendanceStatus;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.AttendanceRepository;
import com.crm.core.repository.LessonRepository;
import com.crm.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final LessonRepository lessonRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public List<Attendance> saveBulkAttendance(UUID lessonId, List<AttendanceDto> dtos) {
        // Урок ищется только в текущей организации (@TenantId)
        if (!lessonRepository.existsById(lessonId)) {
            throw new NotFoundException("Урок не найден");
        }

        List<Attendance> existing = attendanceRepository.findAllByLessonId(lessonId);
        attendanceRepository.deleteAll(existing);
        attendanceRepository.flush();

        List<Attendance> listToSave = dtos.stream().map(dto -> {
            Attendance attendance = Attendance.builder()
                    .lessonId(lessonId)
                    .studentId(dto.getStudentId())
                    .status(dto.getStatus())
                    .grade(dto.getGrade())
                    .note(dto.getNote())
                    .build();

            if (dto.getStatus() == AttendanceStatus.PRESENT) {
                subscriptionRepository.findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtAsc(dto.getStudentId())
                        .ifPresent(sub -> {
                            sub.setLessonsRemaining(sub.getLessonsRemaining() - 1);
                            if (sub.getLessonsRemaining() <= 0) {
                                sub.setActive(false);
                            }
                            subscriptionRepository.save(sub);
                        });
            }
            return attendance;
        }).toList();

        return attendanceRepository.saveAll(listToSave);
    }
}
