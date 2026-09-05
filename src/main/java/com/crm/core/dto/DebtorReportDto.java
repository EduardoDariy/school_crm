package com.crm.core.dto;

import java.util.UUID;

public record DebtorReportDto(
        UUID studentId,
        String firstName,
        String lastName,
        String phoneNumber,
        Integer lessonsRemaining
) {}