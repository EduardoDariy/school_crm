package com.crm.core.dto;

import com.crm.core.entity.enums.AttendanceStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class AttendanceDto {
    private UUID studentId;
    private AttendanceStatus status;
    private Integer grade;
    private String note;
}