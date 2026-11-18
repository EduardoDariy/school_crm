package com.crm.core.controller;

import com.crm.core.dto.user.CreateUserRequest;
import com.crm.core.dto.user.SetPasswordRequest;
import com.crm.core.dto.user.UpdateUserRequest;
import com.crm.core.dto.user.UserView;
import com.crm.core.entity.LoginAttempt;
import com.crm.core.service.LoginJournalService;
import com.crm.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Управление сотрудниками школы — только администратор школы. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final LoginJournalService loginJournalService;

    @GetMapping
    public List<UserView> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    public UserView get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserView update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/block")
    public UserView block(@PathVariable UUID id) {
        return userService.setBlocked(id, true);
    }

    @PostMapping("/{id}/unblock")
    public UserView unblock(@PathVariable UUID id) {
        return userService.setBlocked(id, false);
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@PathVariable UUID id, @Valid @RequestBody SetPasswordRequest request) {
        userService.setPassword(id, request.password());
    }

    /** Журнал входов сотрудников этой школы, новые записи первыми. */
    @GetMapping("/login-journal")
    public Page<LoginAttempt> loginJournal(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        return loginJournalService.currentOrganizationJournal(PageRequest.of(page, Math.min(size, 200)));
    }
}
