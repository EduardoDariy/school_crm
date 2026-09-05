package com.crm.core.service;

import com.crm.core.dto.SubscriptionDto;
import com.crm.core.entity.Subscription;
import com.crm.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Subscription createSubscription(UUID tenantId, SubscriptionDto dto) {
        if (dto.getLessonsTotal() <= 0) {
            throw new IllegalArgumentException("Количество занятий должно быть больше нуля");
        }

        Subscription subscription = Subscription.builder()
                .studentId(dto.getStudentId())
                .groupId(dto.getGroupId())
                .lessonsTotal(dto.getLessonsTotal())
                .lessonsRemaining(dto.getLessonsTotal()) // При старте остаток равен общему числу
                .isActive(true)
                .build();

        subscription.setTenantId(tenantId);
        return subscriptionRepository.save(subscription);
    }

    public List<Subscription> getActiveSubscriptions(UUID tenantId, UUID studentId) {
        return subscriptionRepository.findAllByTenantIdAndStudentIdAndIsActiveTrue(tenantId, studentId);
    }

}
