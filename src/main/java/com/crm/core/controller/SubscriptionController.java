package com.crm.core.controller;

import com.crm.core.dto.SubscriptionDto;
import com.crm.core.entity.Subscription;
import com.crm.core.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public Subscription createSubscription(@RequestBody SubscriptionDto dto) {
        return subscriptionService.createSubscription(dto);
    }

    @GetMapping("/student/{studentId}")
    public List<Subscription> getStudentSubscriptions(@PathVariable UUID studentId) {
        return subscriptionService.getActiveSubscriptions(studentId);
    }
}
