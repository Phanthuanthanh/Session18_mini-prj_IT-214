package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByIdentityCardNumber(String identityCardNumber);
    boolean existsByIdentityCardNumber(String identityCardNumber);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
