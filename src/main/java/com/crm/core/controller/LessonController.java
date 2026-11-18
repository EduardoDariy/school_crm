package com.crm.core.controller;

import com.crm.core.dto.LessonDto;
import com.crm.core.dto.ScheduleGenerationDto;
import com.crm.core.entity.Lesson;
import com.crm.core.service.LessonGeneratorService;
import com.crm.core.service.LessonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Расписание", description = "Занятия группы; администратор")
@RestController
@RequestMapping("/api/groups/{groupId}/lessons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LessonController {

    private final LessonService lessonService;
    private final LessonGeneratorService lessonGeneratorService;

    @PostMapping
    public Lesson createLesson(@PathVariable UUID groupId, @RequestBody LessonDto dto) {
        return lessonService.createSingleLesson(groupId, dto);
    }

    @PostMapping("/bulk")
    public List<Lesson> generateSchedule(@PathVariable UUID groupId, @RequestBody ScheduleGenerationDto dto) {
        return lessonService.generateSchedule(groupId, dto);
    }

    /** Генерация с темой урока; группа берётся из пути, а не из тела запроса. */
    @PostMapping("/generate")
    public List<Lesson> generateLessons(@PathVariable UUID groupId, @RequestBody ScheduleGenerationDto dto) {
        return lessonGeneratorService.generateLessons(groupId, dto);
    }
}
