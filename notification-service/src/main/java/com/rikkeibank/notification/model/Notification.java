package com.rikkeibank.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification implements Serializable {
    private String id;
    private Long customerId;
    private String accountNumber;
    private String type; // BALANCE_INCREASE, BALANCE_DECREASE, ROLLBACK_REFUND
    private String title;
    private String message;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    @Builder.Default
    private boolean isRead = false;
}
