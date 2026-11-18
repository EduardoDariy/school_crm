package com.crm.core.service;

import com.crm.core.dto.PersonDto;
import com.crm.core.entity.Person;
import com.crm.core.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public Person createPerson(PersonDto dto) {
        if (dto.getPhoneNumber() != null && personRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
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

        return personRepository.save(person);
    }

    public Page<Person> searchPersons(String search, Pageable pageable) {
        return personRepository.searchPersons(search, pageable);
    }
}
