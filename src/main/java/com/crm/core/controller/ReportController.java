package com.crm.core.controller;

import com.crm.core.dto.DebtorReportDto;
import com.crm.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
public class ReportController {

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/debtors")
    public List<DebtorReportDto> getDebtors() {
        // Для простых выборок без сложной логики допустимо вызывать репозиторий прямо из контроллера
        return subscriptionRepository.findDebtors();
    }
}
