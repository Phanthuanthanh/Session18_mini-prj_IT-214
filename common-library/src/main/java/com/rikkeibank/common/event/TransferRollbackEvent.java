package com.rikkeibank.common.event;

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
public class TransferRollbackEvent implements Serializable {
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String reason;
    private Long senderCustomerId;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
