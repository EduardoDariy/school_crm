package com.crm.core.service;

import com.crm.core.dto.LessonDto;
import com.crm.core.dto.ScheduleGenerationDto;
import com.crm.core.entity.Group;
import com.crm.core.entity.Lesson;
import com.crm.core.entity.enums.LessonStatus;
import com.crm.core.repository.GroupRepository;
import com.crm.core.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final GroupRepository groupRepository;

    public Lesson createSingleLesson(UUID tenantId, UUID groupId, LessonDto dto) {
        verifyGroupOwnership(tenantId, groupId);

        Lesson lesson = Lesson.builder()
                .groupId(groupId)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(LessonStatus.PLANNED)
                .topic(dto.getTopic())
                .build();
        lesson.setTenantId(tenantId);

        return lessonRepository.save(lesson);
    }

    @Transactional
    public List<Lesson> generateSchedule(UUID tenantId, UUID groupId, ScheduleGenerationDto dto) {
        verifyGroupOwnership(tenantId, groupId);

        List<Lesson> lessons = new ArrayList<>();
        LocalDate currentDate = dto.getStartDate();

        while (!currentDate.isAfter(dto.getEndDate())) {
            if (dto.getDaysOfWeek().contains(currentDate.getDayOfWeek())) {
                Lesson lesson = Lesson.builder()
                        .groupId(groupId)
                        .startTime(LocalDateTime.of(currentDate, dto.getStartTime()))
                        .endTime(LocalDateTime.of(currentDate, dto.getEndTime()))
                        .status(LessonStatus.PLANNED)
                        .build();
                lesson.setTenantId(tenantId);
                lessons.add(lesson);
            }
            currentDate = currentDate.plusDays(1);
        }

        return lessonRepository.saveAll(lessons);
    }

    private void verifyGroupOwnership(UUID tenantId, UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));
        if (!group.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Ошибка доступа: группа принадлежит другой организации");
        }
    }
}