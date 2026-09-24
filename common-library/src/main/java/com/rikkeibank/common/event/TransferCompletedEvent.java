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
public class TransferCompletedEvent implements Serializable {
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String description;
    private Long senderCustomerId;
    private Long receiverCustomerId;
    private BigDecimal senderNewBalance;
    private BigDecimal receiverNewBalance;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
