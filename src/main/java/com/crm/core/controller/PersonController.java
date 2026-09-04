package com.crm.core.controller;

import com.crm.core.dto.PersonDto;
import com.crm.core.entity.Person;
import com.crm.core.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    public Person createPerson(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestBody PersonDto dto) {
        return personService.createPerson(tenantId, dto);
    }
}