package com.rikkeibank.account.service;

import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.common.dto.AccountDto;
import com.rikkeibank.common.dto.CompensateRequest;
import com.rikkeibank.common.dto.CreditRequest;
import com.rikkeibank.common.dto.DebitRequest;
import com.rikkeibank.common.enums.AccountStatus;
import com.rikkeibank.common.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTypeService accountTypeService;

    @InjectMocks
    private AccountService accountService;

    private Account mockAccount;
    private AccountType mockAccountType;

    @BeforeEach
    void setUp() {
        mockAccountType = AccountType.builder()
                .id(1L)
                .typeCode("CHECKING")
                .minBalance(new BigDecimal("50000"))
                .build();

        mockAccount = Account.builder()
                .id(1L)
                .accountNumber("1011223344")
                .customerId(1L)
                .accountType(mockAccountType)
                .balance(new BigDecimal("1000000")) // 1,000,000 VND
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Debit successfully when sufficient balance")
    void testDebit_Success() {
        when(accountRepository.findByAccountNumber("1011223344")).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DebitRequest request = DebitRequest.builder()
                .accountNumber("1011223344")
                .amount(new BigDecimal("200000"))
                .transactionId("TX123")
                .build();

        AccountDto result = accountService.debit(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("800000"), result.getBalance());
        verify(accountRepository, times(1)).save(mockAccount);
    }

    @Test
    @DisplayName("Debit throws InsufficientBalanceException when balance is too low")
    void testDebit_InsufficientBalance() {
        when(accountRepository.findByAccountNumber("1011223344")).thenReturn(Optional.of(mockAccount));

        DebitRequest request = DebitRequest.builder()
                .accountNumber("1011223344")
                .amount(new BigDecimal("990000")) // 1,000,000 - 990,000 = 10,000 < minBalance (50,000)
                .transactionId("TX123")
                .build();

        assertThrows(InsufficientBalanceException.class, () -> accountService.debit(request));
        verify(accountRepository, never()).save(mockAccount);
    }

    @Test
    @DisplayName("Credit successfully adds balance to account")
    void testCredit_Success() {
        when(accountRepository.findByAccountNumber("1011223344")).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditRequest request = CreditRequest.builder()
                .accountNumber("1011223344")
                .amount(new BigDecimal("500000"))
                .transactionId("TX123")
                .build();

        AccountDto result = accountService.credit(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500000"), result.getBalance());
        verify(accountRepository, times(1)).save(mockAccount);
    }

    @Test
    @DisplayName("Saga Compensating debit restores deducted amount")
    void testCompensateDebit_Success() {
        when(accountRepository.findByAccountNumber("1011223344")).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompensateRequest request = CompensateRequest.builder()
                .accountNumber("1011223344")
                .amount(new BigDecimal("200000"))
                .transactionId("TX123")
                .reason("Step 2 failed")
                .build();

        AccountDto result = accountService.compensateDebit(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("1200000"), result.getBalance());
        verify(accountRepository, times(1)).save(mockAccount);
    }
}
