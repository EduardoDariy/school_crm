package com.crm.core.service;

import com.crm.core.dto.PersonDto;
import com.crm.core.entity.Person;
import com.crm.core.exception.DuplicateException;
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
        String phone = PhoneNumbers.normalize(dto.getPhoneNumber());
        if (phone != null) {
            personRepository.findFirstByPhoneNumber(phone).ifPresent(existing -> {
                throw new DuplicateException("Клиент с таким номером телефона уже существует", existing.getId());
            });
        }

        Person person = Person.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phoneNumber(phone)
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
