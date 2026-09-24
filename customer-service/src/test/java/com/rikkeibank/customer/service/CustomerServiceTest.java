package com.rikkeibank.customer.service;

import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        mockCustomer = Customer.builder()
                .id(1L)
                .userId(3L)
                .fullName("Nguyễn Văn An")
                .email("customer1@rikkeibank.com")
                .phone("0912345678")
                .identityCardNumber("001200000001")
                .address("Hà Nội")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Get customer by ID returns customer when exists")
    void testGetCustomerById_Success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));

        Customer result = customerService.getCustomerById(1L);

        assertNotNull(result);
        assertEquals("Nguyễn Văn An", result.getFullName());
    }

    @Test
    @DisplayName("Get customer by ID throws ResourceNotFoundException when not found")
    void testGetCustomerById_NotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerById(99L));
    }

    @Test
    @DisplayName("Create customer successfully when valid")
    void testCreateCustomer_Success() {
        when(customerRepository.existsByIdentityCardNumber("001200000001")).thenReturn(false);
        when(customerRepository.existsByEmail("customer1@rikkeibank.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(mockCustomer);

        Customer result = customerService.createCustomer(mockCustomer);

        assertNotNull(result);
        assertEquals("001200000001", result.getIdentityCardNumber());
    }

    @Test
    @DisplayName("Create customer throws BadRequestException when CCCD exists")
    void testCreateCustomer_DuplicateCCCD() {
        when(customerRepository.existsByIdentityCardNumber("001200000001")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> customerService.createCustomer(mockCustomer));
    }
}
