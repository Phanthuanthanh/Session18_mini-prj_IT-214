package com.rikkeibank.account.entity;

import com.rikkeibank.common.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber; // e.g. "1011223344"

    @Column(nullable = false)
    private Long customerId; // Linked to customer-service

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status; // ACTIVE, FROZEN, CLOSED

    @Builder.Default
    private LocalDateTime openedDate = LocalDateTime.now();
}
