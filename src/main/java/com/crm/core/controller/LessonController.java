package com.crm.core.controller;

import com.crm.core.dto.LessonDto;
import com.crm.core.dto.ScheduleGenerationDto;
import com.crm.core.entity.Lesson;
import com.crm.core.service.LessonGeneratorService;
import com.crm.core.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    public Lesson createLesson(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID groupId,
            @RequestBody LessonDto dto) {
        return lessonService.createSingleLesson(tenantId, groupId, dto);
    }

    @PostMapping("/bulk")
    public List<Lesson> generateSchedule(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID groupId,
            @RequestBody ScheduleGenerationDto dto) {
        return lessonService.generateSchedule(tenantId, groupId, dto);
    }

    private final LessonGeneratorService lessonGeneratorService; // Не забудьте добавить в зависимости контроллера

    @PostMapping("/generate")
    public List<Lesson> generateSchedule(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestBody ScheduleGenerationDto dto) {
        return lessonGeneratorService.generateLessons(tenantId, dto);
    }
}