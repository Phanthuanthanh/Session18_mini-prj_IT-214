package com.rikkeibank.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String identityCardNumber;
    private String address;
    private LocalDate dateOfBirth;
    private String status;
    private Long assignedTellerId;
    private LocalDateTime createdAt;
}
