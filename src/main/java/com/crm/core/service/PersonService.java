package com.crm.core.service;

import com.crm.core.dto.PersonDto;
import com.crm.core.entity.Person;
import com.crm.core.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public Person createPerson(UUID tenantId, PersonDto dto) {
        if (dto.getPhoneNumber() != null &&
                personRepository.existsByTenantIdAndPhoneNumber(tenantId, dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Клиент с таким номером телефона уже существует");
        }

        Person person = Person.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phoneNumber(dto.getPhoneNumber())
                .messengerId(dto.getMessengerId())
                .isStudent(dto.isStudent())
                .isPayer(dto.isPayer())
                .isArchived(false)
                .build();

        person.setTenantId(tenantId);

        return personRepository.save(person);
    }

    public Page<Person> searchPersons(UUID tenantId, String search, org.springframework.data.domain.Pageable pageable) {
        return personRepository.searchPersons(tenantId, search, pageable);
    }
}