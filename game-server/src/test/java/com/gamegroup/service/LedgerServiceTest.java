package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.dto.CreateLedgerRecordRequest;
import com.gamegroup.entity.LedgerAccount;
import com.gamegroup.entity.LedgerCategory;
import com.gamegroup.entity.LedgerRecord;
import com.gamegroup.repository.LedgerAccountRepository;
import com.gamegroup.repository.LedgerCategoryRepository;
import com.gamegroup.repository.LedgerRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerCategoryRepository ledgerCategoryRepository;

    @Mock
    private LedgerRecordRepository ledgerRecordRepository;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void should_create_income_record_and_increase_balance() {
        Long userId = 100L;
        LedgerAccount account = buildAccount(1L, userId, new BigDecimal("100.00"));
        LedgerCategory category = buildCategory(2L, 2);
        CreateLedgerRecordRequest request = buildRequest(1L, 2L, 2, new BigDecimal("20.50"));

        when(ledgerAccountRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(account));
        when(ledgerCategoryRepository.findByIdAndUserIdIn(2L, List.of(0L, userId))).thenReturn(Optional.of(category));
        when(ledgerRecordRepository.save(any(LedgerRecord.class))).thenAnswer(invocation -> {
            LedgerRecord record = invocation.getArgument(0);
            record.setId(99L);
            return record;
        });

        ledgerService.createRecord(userId, request);

        assertEquals(new BigDecimal("120.50"), account.getBalance());
        verify(ledgerAccountRepository, times(1)).save(account);
        verify(ledgerRecordRepository, times(1)).save(any(LedgerRecord.class));
    }

    @Test
    void should_allow_expense_when_balance_insufficient() {
        Long userId = 100L;
        LedgerAccount account = buildAccount(1L, userId, new BigDecimal("10.00"));
        LedgerCategory category = buildCategory(2L, 1);
        CreateLedgerRecordRequest request = buildRequest(1L, 2L, 1, new BigDecimal("20.00"));

        when(ledgerAccountRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(account));
        when(ledgerCategoryRepository.findByIdAndUserIdIn(2L, List.of(0L, userId))).thenReturn(Optional.of(category));
        when(ledgerRecordRepository.save(any(LedgerRecord.class))).thenAnswer(invocation -> {
            LedgerRecord record = invocation.getArgument(0);
            record.setId(99L);
            return record;
        });

        ledgerService.createRecord(userId, request);

        assertEquals(new BigDecimal("-10.00"), account.getBalance());
        verify(ledgerAccountRepository, times(1)).save(account);
        verify(ledgerRecordRepository, times(1)).save(any(LedgerRecord.class));
    }

    @Test
    void should_throw_when_record_type_not_match_category_type() {
        Long userId = 100L;
        LedgerAccount account = buildAccount(1L, userId, new BigDecimal("100.00"));
        LedgerCategory category = buildCategory(2L, 2);
        CreateLedgerRecordRequest request = buildRequest(1L, 2L, 1, new BigDecimal("5.00"));

        when(ledgerAccountRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(account));
        when(ledgerCategoryRepository.findByIdAndUserIdIn(2L, List.of(0L, userId))).thenReturn(Optional.of(category));

        assertThrows(BusinessException.class, () -> ledgerService.createRecord(userId, request));
        verify(ledgerRecordRepository, times(0)).save(any(LedgerRecord.class));
    }

    @Test
    void should_delete_income_record_and_reduce_balance() {
        Long userId = 100L;
        LedgerAccount account = buildAccount(1L, userId, new BigDecimal("80.00"));
        LedgerRecord record = new LedgerRecord();
        record.setId(10L);
        record.setUserId(userId);
        record.setAccountId(1L);
        record.setType(2);
        record.setAmount(new BigDecimal("30.00"));

        when(ledgerRecordRepository.findByIdAndUserId(10L, userId)).thenReturn(Optional.of(record));
        when(ledgerAccountRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(account));

        ledgerService.deleteRecord(userId, 10L);

        assertEquals(new BigDecimal("50.00"), account.getBalance());
        verify(ledgerAccountRepository, times(1)).save(account);
        verify(ledgerRecordRepository, times(1)).delete(record);
    }

    private LedgerAccount buildAccount(Long accountId, Long userId, BigDecimal balance) {
        LedgerAccount account = new LedgerAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setName("现金");
        account.setType("Cash");
        account.setBalance(balance);
        account.setSortOrder(1);
        return account;
    }

    private LedgerCategory buildCategory(Long categoryId, Integer type) {
        LedgerCategory category = new LedgerCategory();
        category.setId(categoryId);
        category.setUserId(0L);
        category.setName("分类");
        category.setType(type);
        return category;
    }

    private CreateLedgerRecordRequest buildRequest(Long accountId, Long categoryId, Integer type, BigDecimal amount) {
        CreateLedgerRecordRequest request = new CreateLedgerRecordRequest();
        request.setAccountId(accountId);
        request.setCategoryId(categoryId);
        request.setType(type);
        request.setAmount(amount);
        request.setRecordTime(LocalDateTime.now());
        request.setRemark("test");
        return request;
    }
}
