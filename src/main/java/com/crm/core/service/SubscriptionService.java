package com.crm.core.service;

import com.crm.core.dto.SubscriptionDto;
import com.crm.core.entity.Subscription;
import com.crm.core.exception.NotFoundException;
import com.crm.core.repository.PersonRepository;
import com.crm.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PersonRepository personRepository;

    public Subscription createSubscription(SubscriptionDto dto) {
        if (dto.getLessonsTotal() == null || dto.getLessonsTotal() <= 0) {
            throw new IllegalArgumentException("Количество занятий должно быть больше нуля");
        }
        // Ученик должен принадлежать текущей организации
        if (dto.getStudentId() == null || !personRepository.existsById(dto.getStudentId())) {
            throw new NotFoundException("Ученик не найден");
        }

        Subscription subscription = Subscription.builder()
                .studentId(dto.getStudentId())
                .groupId(dto.getGroupId())
                .lessonsTotal(dto.getLessonsTotal())
                .lessonsRemaining(dto.getLessonsTotal()) // При старте остаток равен общему числу
                .isActive(true)
                .build();

        return subscriptionRepository.save(subscription);
    }

    public List<Subscription> getActiveSubscriptions(UUID studentId) {
        return subscriptionRepository.findAllByStudentIdAndIsActiveTrue(studentId);
    }
}
