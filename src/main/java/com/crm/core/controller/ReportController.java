package com.crm.core.controller;

import com.crm.core.dto.DebtorReportDto;
import com.crm.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/debtors")
    public List<DebtorReportDto> getDebtors(@RequestHeader("X-Tenant-ID") UUID tenantId) {
        // Для простых выборок без сложной логики допустимо вызывать репозиторий прямо из контроллера
        return subscriptionRepository.findDebtorsByTenantId(tenantId);
    }
}