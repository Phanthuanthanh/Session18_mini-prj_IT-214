package com.rikkeibank.common.dto;

import com.rikkeibank.common.enums.TransactionStatus;
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
public class TransactionResponse {
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String description;
    private TransactionStatus status;
    private String failureReason;
    private Long tellerId;
    private LocalDateTime createdAt;
}
