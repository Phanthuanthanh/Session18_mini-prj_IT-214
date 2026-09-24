package com.rikkeibank.account.config;

import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.account.repository.AccountTypeRepository;
import com.rikkeibank.common.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountDataInitializer implements CommandLineRunner {

    private final AccountTypeRepository accountTypeRepository;
    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountTypeRepository.count() == 0) {
            log.info("Seeding account types...");
            AccountType checking = accountTypeRepository.save(AccountType.builder()
                    .typeCode("CHECKING")
                    .typeName("Tài khoản thanh toán Rikkei-Pay")
                    .interestRate(0.2)
                    .minBalance(new BigDecimal("50000"))
                    .description("Tài khoản phục vụ thanh toán, chuyển khoản online 24/7")
                    .status("ACTIVE")
                    .build());

            AccountType savings = accountTypeRepository.save(AccountType.builder()
                    .typeCode("SAVINGS")
                    .typeName("Tài khoản tiết kiệm có kỳ hạn")
                    .interestRate(6.5)
                    .minBalance(new BigDecimal("1000000"))
                    .description("Tài khoản gửi góp sinh lời cao")
                    .status("ACTIVE")
                    .build());

            if (accountRepository.count() == 0) {
                log.info("Seeding demo accounts...");
                // Account for customer 1 (Nguyễn Văn An)
                accountRepository.save(Account.builder()
                        .accountNumber("1011223344")
                        .customerId(1L)
                        .accountType(checking)
                        .balance(new BigDecimal("10000000.00")) // 10,000,000 VND
                        .currency("VND")
                        .status(AccountStatus.ACTIVE)
                        .build());

                // Account for customer 2 (Trần Thị Bình)
                accountRepository.save(Account.builder()
                        .accountNumber("1022334455")
                        .customerId(2L)
                        .accountType(checking)
                        .balance(new BigDecimal("5000000.00")) // 5,000,000 VND
                        .currency("VND")
                        .status(AccountStatus.ACTIVE)
                        .build());

                log.info("Initialized default accounts: 1011223344 (10M VND), 1022334455 (5M VND)");
            }
        }
    }
}
