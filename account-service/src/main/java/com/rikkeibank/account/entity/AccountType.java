package com.rikkeibank.account.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String typeCode; // e.g., "CHECKING", "SAVINGS", "SALARY"

    @Column(nullable = false, length = 100)
    private String typeName; // e.g., "Tài khoản thanh toán", "Tài khoản tiết kiệm"

    private Double interestRate; // Lãi suất % (vd: 0.1% hoặc 6.5%/năm)

    @Column(nullable = false)
    private BigDecimal minBalance; // Số dư tối thiểu duy trì (vd: 50,000 VND)

    private String description;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
