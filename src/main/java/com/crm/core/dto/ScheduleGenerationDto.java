package com.crm.core.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ScheduleGenerationDto(
        UUID groupId,
        LocalDate startDate,
        LocalDate endDate,
        List<DayOfWeek> daysOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String topicTemplate
) {}