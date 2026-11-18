package com.crm.core.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class GroupDto {
    private String name;
    /** Сотрудник с ролью TEACHER этой же школы; необязательно. */
    private UUID teacherId;
}
