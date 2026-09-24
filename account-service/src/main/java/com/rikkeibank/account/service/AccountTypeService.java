package com.rikkeibank.account.service;

import com.rikkeibank.account.entity.AccountType;
import com.rikkeibank.account.repository.AccountTypeRepository;
import com.rikkeibank.common.exception.BadRequestException;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;

    @Cacheable(value = "accountTypes")
    public List<AccountType> getAllAccountTypes() {
        log.info("Fetching all account types from database (Cache Miss)");
        return accountTypeRepository.findAll();
    }

    @Cacheable(value = "accountType", key = "#id")
    public AccountType getAccountTypeById(Long id) {
        log.info("Fetching account type id={} from database (Cache Miss)", id);
        return accountTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại tài khoản với ID: " + id));
    }

    @Transactional
    @CacheEvict(value = {"accountTypes", "accountType"}, allEntries = true)
    public AccountType createAccountType(AccountType accountType) {
        if (accountTypeRepository.existsByTypeCode(accountType.getTypeCode())) {
            throw new BadRequestException("Mã loại tài khoản đã tồn tại: " + accountType.getTypeCode());
        }
        log.info("Creating new account type: code={}", accountType.getTypeCode());
        return accountTypeRepository.save(accountType);
    }

    @Transactional
    @CacheEvict(value = {"accountTypes", "accountType"}, allEntries = true)
    public AccountType updateAccountType(Long id, AccountType updated) {
        AccountType existing = getAccountTypeById(id);
        existing.setTypeName(updated.getTypeName());
        existing.setInterestRate(updated.getInterestRate());
        existing.setMinBalance(updated.getMinBalance());
        existing.setDescription(updated.getDescription());
        existing.setStatus(updated.getStatus());
        log.info("Updated account type id={}, cache evicted", id);
        return accountTypeRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = {"accountTypes", "accountType"}, allEntries = true)
    public void deleteAccountType(Long id) {
        AccountType existing = getAccountTypeById(id);
        accountTypeRepository.delete(existing);
        log.info("Deleted account type id={}, cache evicted", id);
    }
}
