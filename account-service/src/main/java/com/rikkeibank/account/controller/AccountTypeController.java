package com.rikkeibank.account.controller;

import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.service.AccountTypeService;
import com.rikkeibank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account-types")
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountType>>> getAllAccountTypes() {
        return ResponseEntity.ok(ApiResponse.success(accountTypeService.getAllAccountTypes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountType>> getAccountTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(accountTypeService.getAccountTypeById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountType>> createAccountType(@Valid @RequestBody AccountType accountType) {
        AccountType created = accountTypeService.createAccountType(accountType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo mới danh mục loại tài khoản thành công", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountType>> updateAccountType(
            @PathVariable Long id,
            @Valid @RequestBody AccountType accountType) {
        AccountType updated = accountTypeService.updateAccountType(id, accountType);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật loại tài khoản thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccountType(@PathVariable Long id) {
        accountTypeService.deleteAccountType(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa loại tài khoản thành công", null));
    }
}
