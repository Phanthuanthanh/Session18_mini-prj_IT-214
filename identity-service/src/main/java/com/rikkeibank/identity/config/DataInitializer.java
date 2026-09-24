package com.rikkeibank.identity.config;

import com.rikkeibank.common.enums.UserRole;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial users for RikkeiBank...");

            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@rikkeibank.com")
                    .role(UserRole.ADMIN)
                    .status("ACTIVE")
                    .build());

            userRepository.save(User.builder()
                    .username("teller1")
                    .password(passwordEncoder.encode("teller123"))
                    .email("teller1@rikkeibank.com")
                    .role(UserRole.TELLER)
                    .status("ACTIVE")
                    .build());

            userRepository.save(User.builder()
                    .username("customer1")
                    .password(passwordEncoder.encode("customer123"))
                    .email("customer1@rikkeibank.com")
                    .role(UserRole.CUSTOMER)
                    .status("ACTIVE")
                    .build());

            userRepository.save(User.builder()
                    .username("customer2")
                    .password(passwordEncoder.encode("customer123"))
                    .email("customer2@rikkeibank.com")
                    .role(UserRole.CUSTOMER)
                    .status("ACTIVE")
                    .build());

            log.info("Initialized default users: admin, teller1, customer1, customer2");
        }
    }
}
