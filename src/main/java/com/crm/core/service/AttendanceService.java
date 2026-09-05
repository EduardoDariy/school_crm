package com.crm.core.service;

import com.crm.core.bot.CrmTelegramBot;
import com.crm.core.dto.AttendanceDto;
import com.crm.core.entity.Attendance;
import com.crm.core.entity.Lesson;
import com.crm.core.entity.enums.AttendanceStatus;
import com.crm.core.repository.AttendanceRepository;
import com.crm.core.repository.LessonRepository;
import com.crm.core.repository.PersonRepository;
import com.crm.core.repository.SubscriptionRepository;
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
    private final SubscriptionRepository subscriptionRepository;
    private final PersonRepository personRepository; // Добавлено
    private final CrmTelegramBot crmTelegramBot;     // Добавлено

    @Transactional
    public List<Attendance> saveBulkAttendance(UUID tenantId, UUID lessonId, List<AttendanceDto> dtos) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Урок не найден"));

        if (!lesson.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Ошибка доступа");
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
            attendance.setTenantId(tenantId);

            if (dto.getStatus() == AttendanceStatus.PRESENT) {
                subscriptionRepository.findFirstByTenantIdAndStudentIdAndIsActiveTrueOrderByCreatedAtAsc(tenantId, dto.getStudentId())
                        .ifPresent(sub -> {
                            sub.setLessonsRemaining(sub.getLessonsRemaining() - 1);
                            if (sub.getLessonsRemaining() <= 0) {
                                sub.setActive(false);
                            }
                            subscriptionRepository.save(sub);

                            // Отправка уведомления в Telegram
                            personRepository.findById(dto.getStudentId()).ifPresent(student -> {
                                if (student.getMessengerId() != null && !student.getMessengerId().isEmpty()) {
                                    String text = String.format("Здравствуйте, %s! Списано 1 занятие. Остаток по абонементу: %d",
                                            student.getFirstName(), sub.getLessonsRemaining());
                                    crmTelegramBot.sendMessage(student.getMessengerId(), text);
                                }
                            });
                        });
            }
            return attendance;
        }).collect(Collectors.toList());

        return attendanceRepository.saveAll(listToSave);
    }
}