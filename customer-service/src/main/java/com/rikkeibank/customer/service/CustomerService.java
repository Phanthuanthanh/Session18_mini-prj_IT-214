package com.rikkeibank.customer.service;

import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.repository.CustomerRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        log.info("Fetching all customers from database...");
        return customerRepository.findAll();
    }

    @Cacheable(value = "customers", key = "#id", unless = "#result == null")
    public Customer getCustomerById(Long id) {
        log.info("Cache miss! Querying database for customer id={}", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng với ID: " + id));
    }

    public Customer getCustomerByUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin hồ sơ cho tài khoản của bạn"));
    }

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsByIdentityCardNumber(customer.getIdentityCardNumber())) {
            throw new BadRequestException("Số CCCD/CMND đã tồn tại trong hệ thống");
        }
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new BadRequestException("Email khách hàng đã được sử dụng");
        }
        log.info("Creating new customer: fullName={}", customer.getFullName());
        return customerRepository.save(customer);
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#id")
    public Customer updateCustomer(Long id, Customer updated) {
        Customer existing = getCustomerById(id);
        existing.setFullName(updated.getFullName());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setStatus(updated.getStatus());
        existing.setAssignedTellerId(updated.getAssignedTellerId());
        log.info("Updated customer id={}, evicted from cache.", id);
        return customerRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#id")
    public void deleteCustomer(Long id) {
        Customer existing = getCustomerById(id);
        customerRepository.delete(existing);
        log.info("Deleted customer id={}, evicted from cache.", id);
    }
}
