package com.rikkeibank.customer.service;

import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    public List<Staff> getAllStaffs() {
        log.info("Fetching all staffs from database...");
        return staffRepository.findAll();
    }

    @Cacheable(value = "staffs", key = "#id", unless = "#result == null")
    public Staff getStaffById(Long id) {
        log.info("Cache miss! Querying database for staff id={}", id);
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhân viên với ID: " + id));
    }

    public Staff getStaffByUserId(Long userId) {
        return staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhân viên tương ứng với tài khoản này"));
    }

    @Transactional
    @CacheEvict(value = "staffs", allEntries = true)
    public Staff createStaff(Staff staff) {
        if (staffRepository.existsByStaffCode(staff.getStaffCode())) {
            throw new BadRequestException("Mã nhân viên đã tồn tại");
        }
        if (staffRepository.existsByEmail(staff.getEmail())) {
            throw new BadRequestException("Email nhân viên đã được sử dụng");
        }
        log.info("Creating new staff: code={}, name={}", staff.getStaffCode(), staff.getFullName());
        return staffRepository.save(staff);
    }

    @Transactional
    @CacheEvict(value = "staffs", key = "#id")
    public Staff updateStaff(Long id, Staff updated) {
        Staff existing = getStaffById(id);
        existing.setFullName(updated.getFullName());
        existing.setPhone(updated.getPhone());
        existing.setBranch(updated.getBranch());
        existing.setDepartment(updated.getDepartment());
        existing.setStatus(updated.getStatus());
        log.info("Updated staff id={}, evicted from cache.", id);
        return staffRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = "staffs", key = "#id")
    public void deleteStaff(Long id) {
        Staff existing = getStaffById(id);
        staffRepository.delete(existing);
        log.info("Deleted staff id={}, evicted from cache.", id);
    }
}
