package com.crm.core.dto;

import lombok.Data;

@Data
public class PersonDto {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String messengerId;
    private boolean isStudent;
    private boolean isPayer;
}