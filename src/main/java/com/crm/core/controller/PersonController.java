package com.crm.core.controller;

import com.crm.core.entity.Person;
import com.crm.core.service.PersonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Клиенты", description = "Карточки людей; администратор, менеджер")
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class PersonController {

    private final PersonService personService;

    @GetMapping
    public Page<Person> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return personService.searchPersons(query, PageRequest.of(page, size));
    }
}
