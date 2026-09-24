package com.rikkeibank.identity.entity;

import com.rikkeibank.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, LOCKED, FORCE_LOGGED_OUT

    private Long minValidTokenTimestamp; // Tokens issued before this epoch millis are revoked!

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
