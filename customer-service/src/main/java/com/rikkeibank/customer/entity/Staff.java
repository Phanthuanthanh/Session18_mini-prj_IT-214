package com.rikkeibank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "staffs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Staff implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId; // Linked to identity-service user id

    @Column(nullable = false, unique = true, length = 20)
    private String staffCode; // e.g. "TEL-001"

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    private String branch; // Chi nhánh (e.g. "RikkeiBank Cầu Giấy")

    private String department; // e.g. "Dịch vụ khách hàng"

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
