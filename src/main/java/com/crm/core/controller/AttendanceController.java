package com.crm.core.controller;

import com.crm.core.dto.AttendanceDto;
import com.crm.core.entity.Attendance;
import com.crm.core.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lessons/{lessonId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public List<Attendance> saveAttendance(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID lessonId,
            @RequestBody List<AttendanceDto> dtos) {
        return attendanceService.saveBulkAttendance(tenantId, lessonId, dtos);
    }
}