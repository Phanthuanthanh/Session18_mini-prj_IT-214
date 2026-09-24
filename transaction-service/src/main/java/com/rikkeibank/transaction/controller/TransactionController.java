package com.rikkeibank.transaction.controller;

import com.rikkeibank.common.constant.SecurityConstants;
import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.dto.TransactionResponse;
import com.rikkeibank.common.dto.TransferRequest;
import com.rikkeibank.common.exception.ForbiddenException;
import com.rikkeibank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> customerTransfer(
            @Valid @RequestBody TransferRequest request) {
        // Customer initiates transfer directly
        TransactionResponse response = transactionService.transfer(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Giao dịch chuyển khoản thành công", response));
    }

    @PostMapping("/teller/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> tellerTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(SecurityConstants.HEADER_USER_ID) Long tellerId,
            @RequestHeader(value = SecurityConstants.HEADER_USER_ROLE, required = false) String role) {
        // Teller initiates transfer on behalf of customer
        TransactionResponse response = transactionService.transfer(request, tellerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Giao dịch viên thực hiện chuyển khoản thành công", response));
    }

    @PostMapping("/simulate-failure-transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> simulateFailureTransfer(
            @Valid @RequestBody TransferRequest request) {
        // Explicitly set simulateFailure to true to test Saga Compensating / Rollback
        request.setSimulateFailure(true);
        TransactionResponse response = transactionService.transfer(request, null);
        return ResponseEntity.ok(ApiResponse.success("Kết quả giao dịch", response));
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountHistory(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAccountHistory(accountNumber)));
    }

    @GetMapping("/teller/daily")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getDailyTransactionsForTeller(
            @RequestHeader(SecurityConstants.HEADER_USER_ID) Long tellerId,
            @RequestHeader(value = SecurityConstants.HEADER_USER_ROLE, required = false) String role) {
        // Teller can only view transactions handled by themselves on current day
        return ResponseEntity.ok(ApiResponse.success(transactionService.getDailyTransactionsForTeller(tellerId)));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByTransactionId(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getByTransactionId(transactionId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @RequestHeader(value = SecurityConstants.HEADER_USER_ROLE, required = false) String role) {
        if (role == null || !role.contains("ADMIN")) {
            throw new ForbiddenException("Chỉ ADMIN mới có quyền xem toàn bộ giao dịch hệ thống");
        }
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAllTransactions()));
    }
}
