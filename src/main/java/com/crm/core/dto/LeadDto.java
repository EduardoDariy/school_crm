package com.crm.core.dto;

import lombok.Data;

@Data
public class LeadDto {
    private String name;
    private String phoneNumber;
    private String messengerId;
    private String source;
}