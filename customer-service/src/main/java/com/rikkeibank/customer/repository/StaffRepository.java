package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUserId(Long userId);
    Optional<Staff> findByStaffCode(String staffCode);
    boolean existsByStaffCode(String staffCode);
    boolean existsByEmail(String email);
}
