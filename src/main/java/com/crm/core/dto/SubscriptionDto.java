package com.crm.core.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SubscriptionDto {
    private UUID studentId;
    private UUID groupId; // Можно передать null, если абонемент общий
    private Integer lessonsTotal;
}