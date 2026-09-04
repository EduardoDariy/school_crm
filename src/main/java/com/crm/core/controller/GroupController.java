package com.crm.core.controller;

import com.crm.core.dto.GroupDto;
import com.crm.core.entity.Group;
import com.crm.core.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public Group createGroup(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestBody GroupDto dto) {
        return groupService.createGroup(tenantId, dto.getName());
    }

    @PostMapping("/{groupId}/enroll/{studentId}")
    public Group enrollStudent(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID groupId,
            @PathVariable UUID studentId) {
        return groupService.enrollStudent(tenantId, groupId, studentId);
    }
}