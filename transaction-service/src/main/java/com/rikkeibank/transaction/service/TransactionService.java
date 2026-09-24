package com.rikkeibank.transaction.service;

import com.rikkeibank.common.dto.TransactionResponse;
import com.rikkeibank.common.dto.TransferRequest;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final SagaTransferOrchestrator sagaOrchestrator;
    private final TransactionRepository transactionRepository;

    public TransactionResponse transfer(TransferRequest request, Long tellerId) {
        Transaction tx = sagaOrchestrator.executeTransferSaga(request, tellerId);
        return mapToDto(tx);
    }

    public TransactionResponse getByTransactionId(String transactionId) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionId));
        return mapToDto(tx);
    }

    public List<TransactionResponse> getAccountHistory(String accountNumber) {
        return transactionRepository.findByFromAccountNumberOrToAccountNumberOrderByCreatedAtDesc(accountNumber, accountNumber)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getDailyTransactionsForTeller(Long tellerId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        log.info("Fetching daily transactions for tellerId={} between {} and {}", tellerId, startOfDay, endOfDay);
        return transactionRepository.findByTellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(tellerId, startOfDay, endOfDay)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToDto(Transaction tx) {
        return TransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .fromAccountNumber(tx.getFromAccountNumber())
                .toAccountNumber(tx.getToAccountNumber())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .description(tx.getDescription())
                .status(tx.getStatus())
                .failureReason(tx.getFailureReason())
                .tellerId(tx.getTellerId())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
