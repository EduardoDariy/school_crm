package com.crm.core.controller;

import com.crm.core.entity.Person;
import com.crm.core.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @GetMapping
    public Page<Person> search(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return personService.searchPersons(tenantId, query, PageRequest.of(page, size));
    }
}