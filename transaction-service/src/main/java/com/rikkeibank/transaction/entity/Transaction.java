package com.rikkeibank.transaction.entity;

import com.rikkeibank.common.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionId; // Unique UUID for idempotency and Saga tracking

    @Column(nullable = false, length = 20)
    private String fromAccountNumber;

    @Column(nullable = false, length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "VND";

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status; // PENDING, DEBITED, COMPLETED, FAILED, FAILED_ROLLEDBACK

    private String failureReason;

    private Long tellerId; // If executed by a Teller / Staff

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
