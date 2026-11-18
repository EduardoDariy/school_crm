package com.crm.core.service;

import com.crm.core.dto.LessonDto;
import com.crm.core.dto.ScheduleGenerationDto;
import com.crm.core.entity.Lesson;
import com.crm.core.entity.enums.LessonStatus;
import com.crm.core.exception.NotFoundException;
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

    public Lesson createSingleLesson(UUID groupId, LessonDto dto) {
        requireGroup(groupId);

        Lesson lesson = Lesson.builder()
                .groupId(groupId)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(LessonStatus.PLANNED)
                .topic(dto.getTopic())
                .build();

        return lessonRepository.save(lesson);
    }

    @Transactional
    public List<Lesson> generateSchedule(UUID groupId, ScheduleGenerationDto dto) {
        requireGroup(groupId);

        List<Lesson> lessons = new ArrayList<>();
        LocalDate currentDate = dto.startDate();

        while (!currentDate.isAfter(dto.endDate())) {
            if (dto.daysOfWeek().contains(currentDate.getDayOfWeek())) {
                lessons.add(Lesson.builder()
                        .groupId(groupId)
                        .startTime(LocalDateTime.of(currentDate, dto.startTime()))
                        .endTime(LocalDateTime.of(currentDate, dto.endTime()))
                        .status(LessonStatus.PLANNED)
                        .build());
            }
            currentDate = currentDate.plusDays(1);
        }

        return lessonRepository.saveAll(lessons);
    }

    /** Группа ищется только в текущей организации (@TenantId), поэтому чужая группа — «не найдена». */
    void requireGroup(UUID groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new NotFoundException("Группа не найдена");
        }
    }
}
