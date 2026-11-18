package com.crm.core.controller;

import com.crm.core.dto.LeadDto;
import com.crm.core.entity.Lead;
import com.crm.core.entity.Person;
import com.crm.core.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public Lead createLead(@RequestBody LeadDto dto) {
        return leadService.createLead(dto);
    }

    @GetMapping
    public List<Lead> getLeads() {
        return leadService.getActiveLeads();
    }

    @PostMapping("/{leadId}/convert")
    public Person convertLeadToStudent(@PathVariable UUID leadId) {
        return leadService.convertLeadToStudent(leadId);
    }
}
