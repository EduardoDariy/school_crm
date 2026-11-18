package com.crm.core.controller;

import com.crm.core.dto.GroupDto;
import com.crm.core.entity.Group;
import com.crm.core.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /** Преподаватель видит только свои группы, финансовый сотрудник — не видит. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    public List<Group> list() {
        return groupService.listActive();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Group createGroup(@RequestBody GroupDto dto) {
        return groupService.createGroup(dto);
    }

    @PostMapping("/{groupId}/enroll/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Group enrollStudent(@PathVariable UUID groupId, @PathVariable UUID studentId) {
        return groupService.enrollStudent(groupId, studentId);
    }
}
