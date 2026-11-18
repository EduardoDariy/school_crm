package com.crm.core.service;

import com.crm.core.dto.user.CreateUserRequest;
import com.crm.core.dto.user.UpdateUserRequest;
import com.crm.core.dto.user.UserView;
import com.crm.core.entity.User;
import com.crm.core.entity.enums.StaffRole;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.UserRepository;
import com.crm.core.security.CurrentUser;
import com.crm.core.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Сотрудники текущей организации. Все запросы ограничены ей через @TenantId. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserView> list() {
        return userRepository.findAllByOrderByFullNameAsc().stream().map(UserView::of).toList();
    }

    public UserView get(UUID id) {
        return UserView.of(find(id));
    }

    @Transactional
    public UserView create(CreateUserRequest request) {
        String email = AuthService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Сотрудник с email " + email + " уже существует");
        }
        PasswordPolicy.validate(request.password());

        User user = User.builder()
                .email(email)
                .fullName(request.fullName().trim())
                .role(request.role())
                .passwordHash(passwordEncoder.encode(request.password()))
                .blocked(false)
                .build();
        return UserView.of(userRepository.saveAndFlush(user)); // flush заполняет createdAt
    }

    @Transactional
    public UserView update(UUID id, UpdateUserRequest request) {
        User user = find(id);
        if (user.getRole() != request.role()) {
            if (isSelf(user)) {
                throw new IllegalArgumentException("Нельзя изменить собственную роль");
            }
            requireAnotherActiveAdminIfRemoving(user);
        }
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        return UserView.of(user);
    }

    @Transactional
    public UserView setBlocked(UUID id, boolean blocked) {
        User user = find(id);
        if (blocked) {
            if (isSelf(user)) {
                throw new IllegalArgumentException("Нельзя заблокировать собственную учётную запись");
            }
            requireAnotherActiveAdminIfRemoving(user);
        }
        user.setBlocked(blocked);
        return UserView.of(user);
    }

    /** Администратор задаёт сотруднику новый пароль (например, если тот его забыл). */
    @Transactional
    public void setPassword(UUID id, String password) {
        PasswordPolicy.validate(password);
        find(id).setPasswordHash(passwordEncoder.encode(password));
    }

    @Transactional
    public void changeOwnPassword(String currentPassword, String newPassword) {
        User user = find(CurrentUser.get().id());
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Текущий пароль указан неверно");
        }
        PasswordPolicy.validate(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    /** Если сотрудник — активный администратор, в школе должен остаться хотя бы ещё один. */
    private void requireAnotherActiveAdminIfRemoving(User user) {
        if (user.getRole() == StaffRole.ADMIN && !user.isBlocked()
                && userRepository.countByRoleAndBlockedFalse(StaffRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("В организации должен остаться хотя бы один активный администратор");
        }
    }

    private static boolean isSelf(User user) {
        return user.getId().equals(CurrentUser.get().id());
    }

    private User find(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Сотрудник не найден"));
    }
}
