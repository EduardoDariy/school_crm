package com.crm.core.service;

import com.crm.core.dto.LeadDto;
import com.crm.core.dto.PersonDto;
import com.crm.core.entity.Lead;
import com.crm.core.entity.Person;
import com.crm.core.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final PersonService personService;

    public Lead createLead(UUID tenantId, LeadDto dto) {
        if (dto.getPhoneNumber() != null && leadRepository.existsByTenantIdAndPhoneNumber(tenantId, dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Лид с таким номером телефона уже существует");
        }

        Lead lead = Lead.builder()
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .messengerId(dto.getMessengerId())
                .source(dto.getSource())
                .stageId(UUID.randomUUID())
                .isArchived(false)
                .build();

        lead.setTenantId(tenantId);
        return leadRepository.save(lead);
    }

    public List<Lead> getActiveLeads(UUID tenantId) {
        return leadRepository.findAllByTenantIdAndIsArchivedFalse(tenantId);
    }

    public Person convertLeadToStudent(UUID tenantId, UUID leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Лид не найден"));

        if (!lead.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Ошибка доступа: лид принадлежит другой организации");
        }

        if (lead.isArchived()) {
            throw new IllegalArgumentException("Лид уже находится в архиве");
        }

        String[] nameParts = lead.getName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : null;

        PersonDto personDto = new PersonDto();
        personDto.setFirstName(firstName);
        personDto.setLastName(lastName);
        personDto.setPhoneNumber(lead.getPhoneNumber());
        personDto.setMessengerId(lead.getMessengerId());
        personDto.setStudent(true);

        Person student = personService.createPerson(tenantId, personDto);

        lead.setArchived(true);
        leadRepository.save(lead);

        return student;
    }
}