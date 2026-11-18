package com.crm.core.service;

import com.crm.core.entity.Group;
import com.crm.core.entity.Person;
import com.crm.core.exception.NotFoundException;
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

    public Group createGroup(String name) {
        Group group = Group.builder()
                .name(name)
                .isActive(true)
                .build();
        return groupRepository.save(group);
    }

    @Transactional
    public Group enrollStudent(UUID groupId, UUID studentId) {
        // Оба поиска ограничены текущей организацией (@TenantId)
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Группа не найдена"));
        Person student = personRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Ученик не найден"));

        // @Transactional гарантирует, что JPA сохранит связь в таблицу student_groups
        group.getStudents().add(student);
        return groupRepository.save(group);
    }
}
