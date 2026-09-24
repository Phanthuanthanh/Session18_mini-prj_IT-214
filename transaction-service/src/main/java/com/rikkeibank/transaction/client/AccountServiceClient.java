package com.rikkeibank.transaction.client;

import com.rikkeibank.common.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    ApiResponse<AccountDto> getAccount(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/v1/accounts/debit")
    ApiResponse<AccountDto> debit(@RequestBody DebitRequest request);

    @PostMapping("/api/v1/accounts/credit")
    ApiResponse<AccountDto> credit(@RequestBody CreditRequest request);

    @PostMapping("/api/v1/accounts/compensate-debit")
    ApiResponse<AccountDto> compensateDebit(@RequestBody CompensateRequest request);
}
