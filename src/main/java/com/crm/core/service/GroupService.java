package com.crm.core.service;

import com.crm.core.dto.GroupDto;
import com.crm.core.entity.Group;
import com.crm.core.entity.Person;
import com.crm.core.entity.enums.StaffRole;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.GroupRepository;
import com.crm.core.repository.PersonRepository;
import com.crm.core.repository.UserRepository;
import com.crm.core.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Group> listActive() {
        CurrentUser user = CurrentUser.get();
        List<Group> groups = user.hasRole(StaffRole.TEACHER)
                ? groupRepository.findAllByTeacherIdAndIsActiveTrue(user.id())
                : groupRepository.findAllByIsActiveTrue();
        groups.forEach(group -> group.getStudents().size()); // загрузить учеников до закрытия сессии
        return groups;
    }

    public Group createGroup(GroupDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Название группы обязательно");
        }
        if (dto.getTeacherId() != null) {
            requireTeacher(dto.getTeacherId());
        }
        Group group = Group.builder()
                .name(dto.getName().trim())
                .teacherId(dto.getTeacherId())
                .isActive(true)
                .students(new HashSet<>())
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

    private void requireTeacher(UUID userId) {
        userRepository.findById(userId)
                .filter(user -> user.getRole() == StaffRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Преподаватель не найден"));
    }
}
