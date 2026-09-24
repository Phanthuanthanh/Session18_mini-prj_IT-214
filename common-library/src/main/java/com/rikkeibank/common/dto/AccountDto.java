package com.rikkeibank.common.dto;

import com.rikkeibank.common.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private Long id;
    private String accountNumber;
    private Long customerId;
    private Long accountTypeId;
    private String accountTypeName;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private LocalDateTime openedDate;
}
