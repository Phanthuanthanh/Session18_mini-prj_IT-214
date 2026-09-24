package com.rikkeibank.customer.controller;

import com.rikkeibank.common.constant.SecurityConstants;
import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping("/profile/me")
    public ResponseEntity<ApiResponse<Customer>> getMyProfile(
            @RequestHeader(SecurityConstants.HEADER_USER_ID) Long userId) {
        Customer customer = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@Valid @RequestBody Customer customer) {
        Customer created = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo mới hồ sơ khách hàng thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody Customer customer) {
        Customer updated = customerService.updateCustomer(id, customer);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ khách hàng thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa hồ sơ khách hàng thành công", null));
    }
}
