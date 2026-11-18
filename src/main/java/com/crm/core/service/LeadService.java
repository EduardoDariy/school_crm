package com.crm.core.service;

import com.crm.core.dto.LeadDto;
import com.crm.core.dto.PersonDto;
import com.crm.core.entity.Lead;
import com.crm.core.entity.Person;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final PersonService personService;

    public Lead createLead(LeadDto dto) {
        if (dto.getPhoneNumber() != null && leadRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Лид с таким номером телефона уже существует");
        }

        Lead lead = Lead.builder()
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .messengerId(dto.getMessengerId())
                .source(dto.getSource())
                .stageId(UUID.randomUUID()) // TODO: этап воронки — появится вместе с настраиваемыми воронками
                .isArchived(false)
                .build();

        return leadRepository.save(lead);
    }

    public List<Lead> getActiveLeads() {
        return leadRepository.findAllByIsArchivedFalse();
    }

    @Transactional
    public Person convertLeadToStudent(UUID leadId) {
        // findById уже ограничен текущей организацией: чужой лид просто не будет найден
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Лид не найден"));

        if (lead.isArchived()) {
            throw new IllegalArgumentException("Лид уже находится в архиве");
        }

        String[] nameParts = lead.getName().trim().split("\\s+", 2);

        PersonDto personDto = new PersonDto();
        personDto.setFirstName(nameParts[0]);
        personDto.setLastName(nameParts.length > 1 ? nameParts[1] : null);
        personDto.setPhoneNumber(lead.getPhoneNumber());
        personDto.setMessengerId(lead.getMessengerId());
        personDto.setStudent(true);

        Person student = personService.createPerson(personDto);

        lead.setArchived(true);
        leadRepository.save(lead);

        return student;
    }
}
