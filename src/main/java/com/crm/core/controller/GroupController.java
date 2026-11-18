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
    public Group createGroup(@RequestBody GroupDto dto) {
        return groupService.createGroup(dto.getName());
    }

    @PostMapping("/{groupId}/enroll/{studentId}")
    public Group enrollStudent(@PathVariable UUID groupId, @PathVariable UUID studentId) {
        return groupService.enrollStudent(groupId, studentId);
    }
}
