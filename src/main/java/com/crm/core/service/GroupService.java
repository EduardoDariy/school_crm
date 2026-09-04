package com.crm.core.service;

import com.crm.core.entity.Group;
import com.crm.core.entity.Person;
import com.crm.core.repository.GroupRepository;
import com.crm.core.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;

    public Group createGroup(UUID tenantId, String name) {
        Group group = Group.builder()
                .name(name)
                .isActive(true)
                .build();
        group.setTenantId(tenantId);
        return groupRepository.save(group);
    }

    @Transactional
    public Group enrollStudent(UUID tenantId, UUID groupId, UUID studentId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        if (!group.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Ошибка доступа: группа принадлежит другой организации");
        }

        Person student = personRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Ученик не найден"));

        // Добавляем ученика в коллекцию. Аннотация @Transactional гарантирует,
        // что JPA автоматически сохранит эту связь в таблицу student_groups.
        group.getStudents().add(student);
        return groupRepository.save(group);
    }
}