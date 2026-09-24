package com.rikkeibank.account.controller;

import com.rikkeibank.account.service.AccountService;
import com.rikkeibank.common.dto.*;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAllAccounts()));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountByNumber(accountNumber)));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAccountsByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByCustomerId(customerId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest req) {
        AccountDto account = accountService.createAccount(req.getCustomerId(), req.getAccountTypeId(), req.getInitialDeposit());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Mở tài khoản thành công", account));
    }

    // --- Saga Operations ---

    @PostMapping("/debit")
    public ResponseEntity<ApiResponse<AccountDto>> debit(@Valid @RequestBody DebitRequest request) {
        AccountDto account = accountService.debit(request);
        return ResponseEntity.ok(ApiResponse.success("Trừ tiền thành công", account));
    }

    @PostMapping("/credit")
    public ResponseEntity<ApiResponse<AccountDto>> credit(@Valid @RequestBody CreditRequest request) {
        AccountDto account = accountService.credit(request);
        return ResponseEntity.ok(ApiResponse.success("Cộng tiền thành công", account));
    }

    @PostMapping("/compensate-debit")
    public ResponseEntity<ApiResponse<AccountDto>> compensateDebit(@Valid @RequestBody CompensateRequest request) {
        AccountDto account = accountService.compensateDebit(request);
        return ResponseEntity.ok(ApiResponse.success("Hoàn tiền thành công (Saga Compensating)", account));
    }

    @Data
    public static class CreateAccountRequest {
        private Long customerId;
        private Long accountTypeId;
        private BigDecimal initialDeposit;
    }
}
