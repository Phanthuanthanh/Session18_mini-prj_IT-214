package com.rikkeibank.account.service;

import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.common.dto.AccountDto;
import com.rikkeibank.common.dto.CompensateRequest;
import com.rikkeibank.common.dto.CreditRequest;
import com.rikkeibank.common.dto.DebitRequest;
import com.rikkeibank.common.enums.AccountStatus;
import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.common.exception.InsufficientBalanceException;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTypeService accountTypeService;

    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AccountDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + accountNumber));
        return mapToDto(account);
    }

    public List<AccountDto> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto createAccount(Long customerId, Long accountTypeId, BigDecimal initialDeposit) {
        AccountType accountType = accountTypeService.getAccountTypeById(accountTypeId);

        if (initialDeposit == null) {
            initialDeposit = accountType.getMinBalance();
        } else if (initialDeposit.compareTo(accountType.getMinBalance()) < 0) {
            throw new BadRequestException("Số dư khởi tạo phải lớn hơn hoặc bằng mức tối thiểu: " + accountType.getMinBalance() + " VND");
        }

        String accountNumber = generateUniqueAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customerId(customerId)
                .accountType(accountType)
                .balance(initialDeposit)
                .currency("VND")
                .status(AccountStatus.ACTIVE)
                .build();

        account = accountRepository.save(account);
        log.info("Created account: accountNumber={}, customerId={}, initialBalance={}", accountNumber, customerId, initialDeposit);
        return mapToDto(account);
    }

    @Transactional
    public AccountDto debit(DebitRequest request) {
        log.info("Saga Step 1: Processing DEBIT for account={}, amount={}, txId={}",
                request.getAccountNumber(), request.getAmount(), request.getTransactionId());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản nguồn không tồn tại: " + request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản nguồn đang ở trạng thái: " + account.getStatus() + ", không thể thực hiện giao dịch.");
        }

        BigDecimal minBalance = account.getAccountType() != null ? account.getAccountType().getMinBalance() : BigDecimal.ZERO;
        BigDecimal availableBalance = account.getBalance().subtract(minBalance);

        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Số dư khả dụng không đủ để thực hiện giao dịch. Khả dụng: " + availableBalance + " VND, yêu cầu: " + request.getAmount() + " VND");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        account = accountRepository.save(account);

        log.info("DEBIT successful for account={}, newBalance={}", account.getAccountNumber(), account.getBalance());
        return mapToDto(account);
    }

    @Transactional
    public AccountDto credit(CreditRequest request) {
        log.info("Saga Step 2: Processing CREDIT for account={}, amount={}, txId={}",
                request.getAccountNumber(), request.getAmount(), request.getTransactionId());

        // Simulation flag for testing Saga Compensating / Rollback
        if (request.isSimulateFailure()) {
            log.error("SIMULATED FAILURE: Credit step forced to fail for transaction {}", request.getTransactionId());
            throw new BadRequestException("Mô phỏng lỗi hệ thống ghi có tài khoản đích (Simulated Credit Failure). Kích hoạt Saga Rollback!");
        }

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản đích không tồn tại: " + request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản đích đang ở trạng thái " + account.getStatus() + ", không thể nhận tiền.");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        account = accountRepository.save(account);

        log.info("CREDIT successful for account={}, newBalance={}", account.getAccountNumber(), account.getBalance());
        return mapToDto(account);
    }

    @Transactional
    public AccountDto compensateDebit(CompensateRequest request) {
        log.warn("SAGA COMPENSATING: Rolling back debit for account={}, amount={}, txId={}, reason={}",
                request.getAccountNumber(), request.getAmount(), request.getTransactionId(), request.getReason());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản cần hoàn tiền không tồn tại: " + request.getAccountNumber()));

        account.setBalance(account.getBalance().add(request.getAmount()));
        account = accountRepository.save(account);

        log.info("SAGA COMPENSATING COMPLETED: Restored balance for account={}, balanceAfterRollback={}",
                account.getAccountNumber(), account.getBalance());
        return mapToDto(account);
    }

    private String generateUniqueAccountNumber() {
        Random random = new Random();
        String number;
        do {
            number = "10" + (10000000 + random.nextInt(90000000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountTypeId(account.getAccountType() != null ? account.getAccountType().getId() : null)
                .accountTypeName(account.getAccountType() != null ? account.getAccountType().getTypeName() : null)
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .openedDate(account.getOpenedDate())
                .build();
    }
}
