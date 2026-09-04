package com.crm.core.controller;

import com.crm.core.dto.LeadDto;
import com.crm.core.entity.Lead;
import java.util.List;
import com.crm.core.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import com.crm.core.entity.Person;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public Lead createLead(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestBody LeadDto dto) {
        return leadService.createLead(tenantId, dto);
    }

    @GetMapping
    public List<Lead> getLeads(@RequestHeader("X-Tenant-ID") UUID tenantId) {
        return leadService.getActiveLeads(tenantId);
    }

    @PostMapping("/{leadId}/convert")
    public Person convertLeadToStudent(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID leadId) {
        return leadService.convertLeadToStudent(tenantId, leadId);
    }
}