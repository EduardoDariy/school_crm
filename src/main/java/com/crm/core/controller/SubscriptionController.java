package com.crm.core.controller;

import com.crm.core.dto.SubscriptionDto;
import com.crm.core.entity.Subscription;
import com.crm.core.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public Subscription createSubscription(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestBody SubscriptionDto dto) {
        return subscriptionService.createSubscription(tenantId, dto);
    }

    @GetMapping("/student/{studentId}")
    public List<Subscription> getStudentSubscriptions(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID studentId) {
        return subscriptionService.getActiveSubscriptions(tenantId, studentId);
    }
    
}