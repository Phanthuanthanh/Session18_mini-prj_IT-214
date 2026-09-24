package com.rikkeibank.transaction.service;

import com.rikkeibank.common.dto.*;
import com.rikkeibank.common.enums.TransactionStatus;
import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.transaction.client.AccountServiceClient;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaTransferOrchestratorTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionKafkaProducer kafkaProducer;

    @InjectMocks
    private SagaTransferOrchestrator sagaOrchestrator;

    private TransferRequest request;
    private AccountDto sourceAccount;
    private AccountDto targetAccount;

    @BeforeEach
    void setUp() {
        request = TransferRequest.builder()
                .fromAccountNumber("1011223344")
                .toAccountNumber("1022334455")
                .amount(new BigDecimal("500000"))
                .description("Test transfer")
                .build();

        sourceAccount = AccountDto.builder()
                .accountNumber("1011223344")
                .customerId(1L)
                .balance(new BigDecimal("9500000"))
                .build();

        targetAccount = AccountDto.builder()
                .accountNumber("1022334455")
                .customerId(2L)
                .balance(new BigDecimal("5500000"))
                .build();
    }

    @Test
    @DisplayName("Saga transfer completes successfully when debit and credit succeed")
    void testExecuteTransferSaga_Success() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountServiceClient.debit(any(DebitRequest.class))).thenReturn(ApiResponse.success(sourceAccount));
        when(accountServiceClient.credit(any(CreditRequest.class))).thenReturn(ApiResponse.success(targetAccount));

        Transaction result = sagaOrchestrator.executeTransferSaga(request, null);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        verify(accountServiceClient, times(1)).debit(any(DebitRequest.class));
        verify(accountServiceClient, times(1)).credit(any(CreditRequest.class));
        verify(accountServiceClient, never()).compensateDebit(any(CompensateRequest.class));
        verify(kafkaProducer, times(1)).publishTransferCompleted(any());
    }

    @Test
    @DisplayName("Saga transfer triggers COMPENSATING action (rollback) when credit step fails")
    void testExecuteTransferSaga_CompensatingRollback() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountServiceClient.debit(any(DebitRequest.class))).thenReturn(ApiResponse.success(sourceAccount));
        // Credit fails with exception
        when(accountServiceClient.credit(any(CreditRequest.class))).thenThrow(new RuntimeException("Tài khoản đích bị khóa"));
        when(accountServiceClient.compensateDebit(any(CompensateRequest.class))).thenReturn(ApiResponse.success(sourceAccount));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                sagaOrchestrator.executeTransferSaga(request, null));

        assertTrue(ex.getMessage().contains("Saga đã kích hoạt cơ chế hoàn tiền (Compensating) thành công"));

        // Verify Compensate action was executed!
        verify(accountServiceClient, times(1)).debit(any(DebitRequest.class));
        verify(accountServiceClient, times(1)).credit(any(CreditRequest.class));
        verify(accountServiceClient, times(1)).compensateDebit(any(CompensateRequest.class));
        verify(kafkaProducer, times(1)).publishTransferRollback(any());
    }
}
