package com.crm.core.service;

import com.crm.core.dto.ScheduleGenerationDto;
import com.crm.core.entity.Lesson;
import com.crm.core.entity.enums.LessonStatus;
import com.crm.core.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonGeneratorService {

    private final LessonRepository lessonRepository;

    @Transactional
    public List<Lesson> generateLessons(UUID tenantId, ScheduleGenerationDto dto) {
        List<Lesson> lessonsToSave = new ArrayList<>();

        // Обращение к record идет без приставки get
        LocalDate currentDate = dto.startDate();

        while (!currentDate.isAfter(dto.endDate())) {
            if (dto.daysOfWeek().contains(currentDate.getDayOfWeek())) {
                Lesson lesson = new Lesson();
                lesson.setTenantId(tenantId);
                lesson.setGroupId(dto.groupId());

                lesson.setStartTime(currentDate.atTime(dto.startTime()));
                lesson.setEndTime(currentDate.atTime(dto.endTime()));
                lesson.setStatus(LessonStatus.PLANNED);
                lesson.setTopic(dto.topicTemplate());

                lessonsToSave.add(lesson);
            }
            currentDate = currentDate.plusDays(1);
        }

        return (List<Lesson>) lessonRepository.saveAll(lessonsToSave);
    }
}