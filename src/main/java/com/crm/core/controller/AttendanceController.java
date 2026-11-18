package com.crm.core.controller;

import com.crm.core.dto.AttendanceDto;
import com.crm.core.entity.Attendance;
import com.crm.core.service.AttendanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Посещаемость", description = "Администратор; преподаватель — только свои группы")
@RestController
@RequestMapping("/api/lessons/{lessonId}/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public List<Attendance> saveAttendance(
            @PathVariable UUID lessonId,
            @RequestBody List<AttendanceDto> dtos) {
        return attendanceService.saveBulkAttendance(lessonId, dtos);
    }
}
