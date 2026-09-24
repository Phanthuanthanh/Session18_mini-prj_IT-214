package com.rikkeibank.transaction.repository;

import com.rikkeibank.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByFromAccountNumberOrToAccountNumberOrderByCreatedAtDesc(String fromAccountNumber, String toAccountNumber);

    List<Transaction> findByTellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long tellerId, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
