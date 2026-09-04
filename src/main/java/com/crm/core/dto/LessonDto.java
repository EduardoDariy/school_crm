package com.crm.core.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LessonDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String topic;
}