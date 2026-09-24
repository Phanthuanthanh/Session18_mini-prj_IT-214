package com.rikkeibank.transaction.service;

import com.rikkeibank.common.dto.*;
import com.rikkeibank.common.enums.TransactionStatus;
import com.rikkeibank.common.event.TransferCompletedEvent;
import com.rikkeibank.common.event.TransferRollbackEvent;
import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.transaction.client.AccountServiceClient;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaTransferOrchestrator {

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final TransactionKafkaProducer kafkaProducer;

    @CircuitBreaker(name = "accountServiceCB", fallbackMethod = "transferFallback")
    public Transaction executeTransferSaga(TransferRequest request, Long tellerId) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new BadRequestException("Tài khoản nguồn và tài khoản đích không được trùng nhau");
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        log.info("========== START SAGA ORCHESTRATOR FOR TRANSFER [{}] ==========", transactionId);
        log.info("From: {}, To: {}, Amount: {} VND, SimulateFailure: {}",
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount(), request.isSimulateFailure());

        // 1. Initialize Transaction in DB
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .currency("VND")
                .description(request.getDescription() != null ? request.getDescription() : "Chuyển tiền qua RikkeiBank")
                .status(TransactionStatus.PENDING)
                .tellerId(tellerId)
                .createdAt(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        // 2. Saga Step 1: Debit from source account
        AccountDto debitedAccount;
        try {
            log.info("Saga Step 1: Calling Account-Service to DEBIT account {}", request.getFromAccountNumber());
            DebitRequest debitReq = DebitRequest.builder()
                    .accountNumber(request.getFromAccountNumber())
                    .amount(request.getAmount())
                    .transactionId(transactionId)
                    .build();

            ApiResponse<AccountDto> debitResp = accountServiceClient.debit(debitReq);
            debitedAccount = debitResp.getData();
            transaction.setStatus(TransactionStatus.DEBITED);
            transactionRepository.save(transaction);
            log.info("Saga Step 1 SUCCESS: Debited account {}, remaining balance={}",
                    debitedAccount.getAccountNumber(), debitedAccount.getBalance());

        } catch (Exception e) {
            log.error("Saga Step 1 FAILED: Could not debit account {}: {}", request.getFromAccountNumber(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Lỗi trừ tiền tài khoản nguồn: " + e.getMessage());
            transactionRepository.save(transaction);
            throw new BadRequestException("Giao dịch thất bại: " + e.getMessage());
        }

        // 3. Saga Step 2: Credit to destination account
        AccountDto creditedAccount;
        try {
            log.info("Saga Step 2: Calling Account-Service to CREDIT account {}", request.getToAccountNumber());
            CreditRequest creditReq = CreditRequest.builder()
                    .accountNumber(request.getToAccountNumber())
                    .amount(request.getAmount())
                    .transactionId(transactionId)
                    .simulateFailure(request.isSimulateFailure())
                    .build();

            ApiResponse<AccountDto> creditResp = accountServiceClient.credit(creditReq);
            creditedAccount = creditResp.getData();
            log.info("Saga Step 2 SUCCESS: Credited account {}, new balance={}",
                    creditedAccount.getAccountNumber(), creditedAccount.getBalance());

        } catch (Exception e) {
            log.error("Saga Step 2 FAILED: Could not credit account {}: {}", request.getToAccountNumber(), e.getMessage());
            log.warn(">>> TRIGGERING SAGA COMPENSATING ACTION (ROLLBACK) FOR TX [{}] <<<", transactionId);

            // Step 2 failed -> Execute Saga Compensating Action: Compensate Debit
            try {
                CompensateRequest compReq = CompensateRequest.builder()
                        .accountNumber(request.getFromAccountNumber())
                        .amount(request.getAmount())
                        .transactionId(transactionId)
                        .reason("Rollback do lỗi cộng tiền tài khoản đích: " + e.getMessage())
                        .build();

                ApiResponse<AccountDto> compResp = accountServiceClient.compensateDebit(compReq);
                log.info("Saga COMPENSATING ACTION SUCCESS: Restored balance for account {}, current balance={}",
                        request.getFromAccountNumber(), compResp.getData().getBalance());

                transaction.setStatus(TransactionStatus.FAILED_ROLLEDBACK);
                transaction.setFailureReason("Lỗi ghi có tài khoản đích (" + e.getMessage() + "). Hệ thống Saga đã hoàn tiền về tài khoản nguồn.");
                transactionRepository.save(transaction);

                // Publish Rollback Event to Kafka
                TransferRollbackEvent rollbackEvent = TransferRollbackEvent.builder()
                        .transactionId(transactionId)
                        .fromAccountNumber(request.getFromAccountNumber())
                        .toAccountNumber(request.getToAccountNumber())
                        .amount(request.getAmount())
                        .reason(transaction.getFailureReason())
                        .senderCustomerId(debitedAccount.getCustomerId())
                        .timestamp(LocalDateTime.now())
                        .build();
                kafkaProducer.publishTransferRollback(rollbackEvent);

            } catch (Exception compEx) {
                log.error("CRITICAL SAGA COMPENSATING FAILURE for tx [{}]: {}", transactionId, compEx.getMessage(), compEx);
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("Lỗi nghiêm trọng khi hoàn tiền: " + compEx.getMessage());
                transactionRepository.save(transaction);
            }

            throw new BadRequestException("Giao dịch chuyển khoản thất bại tại bước cộng tiền. Saga đã kích hoạt cơ chế hoàn tiền (Compensating) thành công! Số dư tài khoản "
                    + request.getFromAccountNumber() + " đã được bảo toàn.");
        }

        // 4. Saga All Steps Succeeded!
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);
        log.info("========== SAGA ORCHESTRATOR TRANSFER [{}] COMPLETED SUCCESSFULLY ==========", transactionId);

        // Publish Completed Event to Kafka
        TransferCompletedEvent completedEvent = TransferCompletedEvent.builder()
                .transactionId(transactionId)
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .currency("VND")
                .description(transaction.getDescription())
                .senderCustomerId(debitedAccount.getCustomerId())
                .receiverCustomerId(creditedAccount.getCustomerId())
                .senderNewBalance(debitedAccount.getBalance())
                .receiverNewBalance(creditedAccount.getBalance())
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.publishTransferCompleted(completedEvent);

        return transaction;
    }

    // Circuit Breaker Fallback
    public Transaction transferFallback(TransferRequest request, Long tellerId, Throwable throwable) {
        log.error("Circuit Breaker OPEN or Fallback triggered: Account-Service is unavailable! Error: {}", throwable.getMessage());
        throw new BadRequestException("Hệ thống quản lý tài khoản (Account-Service) tạm thời không khả dụng. Circuit Breaker đã kích hoạt để bảo vệ hệ thống. Chi tiết: " + throwable.getMessage());
    }
}
