package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.CreateLedgerAccountRequest;
import com.gamegroup.dto.CreateLedgerBatchRecordRequest;
import com.gamegroup.dto.CreateLedgerRecordRequest;
import com.gamegroup.dto.LedgerAccountDTO;
import com.gamegroup.dto.LedgerBatchRecordResponse;
import com.gamegroup.dto.LedgerCategoryDTO;
import com.gamegroup.dto.LedgerCategoryGroupDTO;
import com.gamegroup.dto.LedgerCategoryStatsItemDTO;
import com.gamegroup.dto.LedgerCategoryStatsResponse;
import com.gamegroup.dto.LedgerDayGroupDTO;
import com.gamegroup.dto.LedgerOverviewStatsDTO;
import com.gamegroup.dto.LedgerRecordDTO;
import com.gamegroup.dto.LedgerRecordQueryResponse;
import com.gamegroup.dto.LedgerTrendPointDTO;
import com.gamegroup.entity.LedgerAccount;
import com.gamegroup.entity.LedgerCategory;
import com.gamegroup.entity.LedgerRecord;
import com.gamegroup.repository.LedgerAccountRepository;
import com.gamegroup.repository.LedgerCategoryRepository;
import com.gamegroup.repository.LedgerRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private static final Long SYSTEM_USER_ID = 0L;
    private static final Integer TYPE_EXPENSE = 1;
    private static final Integer TYPE_INCOME = 2;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerCategoryRepository ledgerCategoryRepository;
    private final LedgerRecordRepository ledgerRecordRepository;

    @Transactional
    public List<LedgerAccountDTO> getAccounts(Long userId) {
        List<LedgerAccount> accounts = ledgerAccountRepository.findByUserIdOrderBySortOrderAscIdAsc(userId);
        if (accounts.isEmpty() && userId > 0) {
            accounts = copyDefaultAccountsForUser(userId);
        }
        return accounts.stream().map(this::toAccountDTO).toList();
    }

    @Transactional
    public LedgerAccountDTO createAccount(Long userId, CreateLedgerAccountRequest request) {
        LedgerAccount account = new LedgerAccount();
        account.setUserId(userId);
        account.setName(request.getName().trim());
        account.setType(request.getType() == null || request.getType().isBlank() ? "General" : request.getType().trim());
        account.setIcon(request.getIcon());
        account.setBalance(BigDecimal.ZERO);
        account.setSortOrder(nextAccountSortOrder(userId));

        LedgerAccount saved = ledgerAccountRepository.save(account);
        return toAccountDTO(saved);
    }

    public LedgerCategoryGroupDTO getCategories(Long userId) {
        List<LedgerCategory> expense = mergeSystemAndUserCategories(userId, TYPE_EXPENSE);
        List<LedgerCategory> income = mergeSystemAndUserCategories(userId, TYPE_INCOME);
        return LedgerCategoryGroupDTO.builder()
                .expense(expense.stream().map(this::toCategoryDTO).toList())
                .income(income.stream().map(this::toCategoryDTO).toList())
                .build();
    }

    @Transactional
    public LedgerRecordDTO createRecord(Long userId, CreateLedgerRecordRequest request) {
        return createRecordInternal(userId, request, null, null);
    }

    /**
     * 创建周期账生成的记录（会把 recurringRuleId/recurringMonth 写入记录表，用于幂等）。
     */
    @Transactional
    public LedgerRecordDTO createRecurringRecord(
            Long userId,
            CreateLedgerRecordRequest request,
            Long recurringRuleId,
            String recurringMonth
    ) {
        return createRecordInternal(userId, request, recurringRuleId, recurringMonth);
    }

    private LedgerRecordDTO createRecordInternal(
            Long userId,
            CreateLedgerRecordRequest request,
            Long recurringRuleId,
            String recurringMonth
    ) {
        validateType(request.getType());

        LedgerAccount account = ledgerAccountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_ACCOUNT_NOT_FOUND));

        LedgerCategory category = ledgerCategoryRepository.findByIdAndUserIdIn(request.getCategoryId(), List.of(SYSTEM_USER_ID, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_CATEGORY_NOT_FOUND));

        if (!request.getType().equals(category.getType())) {
            throw new BusinessException(ErrorCode.LEDGER_TYPE_MISMATCH);
        }

        LedgerRecord record = new LedgerRecord();
        record.setUserId(userId);
        record.setAccountId(account.getId());
        record.setCategoryId(category.getId());
        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setRecordTime(request.getRecordTime());
        record.setRemark(request.getRemark());
        record.setCategoryName(category.getName());
        record.setCategoryIcon(category.getIcon());
        record.setAccountName(account.getName());
        record.setRecurringRuleId(recurringRuleId);
        record.setRecurringMonth(recurringMonth);

        LedgerRecord saved = ledgerRecordRepository.save(record);
        applyAccountBalanceChange(account, request.getType(), request.getAmount());
        ledgerAccountRepository.save(account);
        log.info("Ledger record created: recordId={}, userId={}, recurringRuleId={}",
                saved.getId(), userId, recurringRuleId);
        return toRecordDTO(saved);
    }

    @Transactional
    public LedgerBatchRecordResponse createBatchRecords(Long userId, CreateLedgerBatchRecordRequest request) {
        validateType(request.getType());

        LedgerAccount account = ledgerAccountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_ACCOUNT_NOT_FOUND));

        LedgerCategory category = ledgerCategoryRepository.findByIdAndUserIdIn(request.getCategoryId(), List.of(SYSTEM_USER_ID, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_CATEGORY_NOT_FOUND));

        if (!request.getType().equals(category.getType())) {
            throw new BusinessException(ErrorCode.LEDGER_TYPE_MISMATCH);
        }

        List<LocalDate> dates = request.getRecordDates().stream()
                .filter(date -> date != null)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (dates.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "至少选择一个有效日期");
        }

        BigDecimal totalAmount = request.getAmount().multiply(BigDecimal.valueOf(dates.size()));

        List<LedgerRecord> records = new ArrayList<>();
        for (LocalDate date : dates) {
            LedgerRecord record = new LedgerRecord();
            record.setUserId(userId);
            record.setAccountId(account.getId());
            record.setCategoryId(category.getId());
            record.setAmount(request.getAmount());
            record.setType(request.getType());
            record.setRecordTime(date.atTime(9, 0));
            record.setRemark(request.getRemark());
            record.setCategoryName(category.getName());
            record.setCategoryIcon(category.getIcon());
            record.setAccountName(account.getName());
            records.add(record);
        }

        ledgerRecordRepository.saveAll(records);
        applyAccountBalanceChange(account, request.getType(), totalAmount);
        ledgerAccountRepository.save(account);
        log.info("Ledger records batch created: userId={}, count={}", userId, records.size());

        return LedgerBatchRecordResponse.builder()
                .createdCount(records.size())
                .totalAmount(totalAmount)
                .dates(dates.stream().map(date -> date.format(DATE_FORMATTER)).toList())
                .build();
    }

    @Transactional
    public void deleteRecord(Long userId, Long recordId) {
        LedgerRecord record = ledgerRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_RECORD_NOT_FOUND));
        LedgerAccount account = ledgerAccountRepository.findByIdAndUserId(record.getAccountId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_ACCOUNT_NOT_FOUND));

        rollbackAccountBalanceChange(account, record.getType(), record.getAmount());
        ledgerAccountRepository.save(account);
        ledgerRecordRepository.delete(record);
        log.info("Ledger record deleted: recordId={}, userId={}", recordId, userId);
    }

    public LedgerRecordQueryResponse queryRecords(
            Long userId,
            String month,
            String startDate,
            String endDate,
            Long accountId,
            Long categoryId,
            int page,
            int size
    ) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        DateRange range = resolveRange(month, startDate, endDate);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        if (accountId != null) {
            ledgerAccountRepository.findByIdAndUserId(accountId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_ACCOUNT_NOT_FOUND));
        }
        if (categoryId != null) {
            ledgerCategoryRepository.findByIdAndUserIdIn(categoryId, List.of(SYSTEM_USER_ID, userId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_CATEGORY_NOT_FOUND));
        }

        Page<LedgerRecord> records = queryRecordPage(userId, range.start(), range.endExclusive(), accountId, categoryId, pageable);
        Map<LocalDate, List<LedgerRecord>> grouped = new LinkedHashMap<>();
        for (LedgerRecord record : records.getContent()) {
            grouped.computeIfAbsent(record.getRecordTime().toLocalDate(), ignored -> new ArrayList<>()).add(record);
        }

        List<LedgerDayGroupDTO> groups = grouped.entrySet().stream().map(entry -> {
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;
            List<LedgerRecordDTO> items = new ArrayList<>();
            for (LedgerRecord record : entry.getValue()) {
                if (TYPE_INCOME.equals(record.getType())) {
                    totalIncome = totalIncome.add(record.getAmount());
                } else {
                    totalExpense = totalExpense.add(record.getAmount());
                }
                items.add(toRecordDTO(record));
            }
            return LedgerDayGroupDTO.builder()
                    .date(entry.getKey().format(DATE_FORMATTER))
                    .totalIncome(totalIncome)
                    .totalExpense(totalExpense)
                    .records(items)
                    .build();
        }).toList();

        return LedgerRecordQueryResponse.builder()
                .groups(groups)
                .total(records.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .build();
    }

    public LedgerOverviewStatsDTO getOverviewStats(Long userId, String month, String startDate, String endDate) {
        DateRange range = resolveRange(month, startDate, endDate);
        List<LedgerRecord> records = findRecordsByRange(userId, range);
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (LedgerRecord record : records) {
            if (TYPE_INCOME.equals(record.getType())) {
                totalIncome = totalIncome.add(record.getAmount());
            } else {
                totalExpense = totalExpense.add(record.getAmount());
            }
        }
        return LedgerOverviewStatsDTO.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .build();
    }

    public List<LedgerTrendPointDTO> getTrendStats(Long userId, String month, String startDate, String endDate) {
        DateRange range = resolveRange(month, startDate, endDate);
        List<LedgerRecord> records = findRecordsByRange(userId, range);
        Map<LocalDate, LedgerTrendPointDTO> dayMap = new LinkedHashMap<>();
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            LocalDate date = cursor;
            dayMap.put(date, LedgerTrendPointDTO.builder()
                    .date(date.format(DATE_FORMATTER))
                    .income(BigDecimal.ZERO)
                    .expense(BigDecimal.ZERO)
                    .build());
            cursor = cursor.plusDays(1);
        }
        for (LedgerRecord record : records) {
            LocalDate date = record.getRecordTime().toLocalDate();
            LedgerTrendPointDTO point = dayMap.get(date);
            if (point == null) {
                continue;
            }
            if (TYPE_INCOME.equals(record.getType())) {
                point.setIncome(point.getIncome().add(record.getAmount()));
            } else {
                point.setExpense(point.getExpense().add(record.getAmount()));
            }
        }
        return dayMap.values().stream().toList();
    }

    public LedgerCategoryStatsResponse getCategoryStats(Long userId, String month, String startDate, String endDate) {
        DateRange range = resolveRange(month, startDate, endDate);
        List<LedgerRecord> records = findRecordsByRange(userId, range);
        return LedgerCategoryStatsResponse.builder()
                .expense(buildCategoryStats(records, TYPE_EXPENSE))
                .income(buildCategoryStats(records, TYPE_INCOME))
                .build();
    }

    private List<LedgerAccount> copyDefaultAccountsForUser(Long userId) {
        List<LedgerAccount> defaults = ledgerAccountRepository.findByUserIdOrderByIdAsc(SYSTEM_USER_ID);
        if (defaults.isEmpty()) {
            return List.of();
        }
        List<LedgerAccount> copied = new ArrayList<>();
        int order = 1;
        for (LedgerAccount item : defaults) {
            LedgerAccount account = new LedgerAccount();
            account.setUserId(userId);
            account.setName(item.getName());
            account.setType(item.getType());
            account.setIcon(item.getIcon());
            account.setBalance(BigDecimal.ZERO);
            account.setSortOrder(order++);
            copied.add(account);
        }
        return ledgerAccountRepository.saveAll(copied);
    }

    private Integer nextAccountSortOrder(Long userId) {
        return ledgerAccountRepository.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .map(LedgerAccount::getSortOrder)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .map(v -> v + 1)
                .orElse(1);
    }

    private List<LedgerCategory> mergeSystemAndUserCategories(Long userId, Integer type) {
        List<LedgerCategory> categories = ledgerCategoryRepository
                .findByUserIdInAndTypeOrderBySortOrderAscIdAsc(List.of(SYSTEM_USER_ID, userId), type);
        if (categories.isEmpty() && userId > 0) {
            return ledgerCategoryRepository.findByUserIdAndTypeOrderBySortOrderAscIdAsc(SYSTEM_USER_ID, type);
        }
        return categories;
    }

    private void validateType(Integer type) {
        if (!TYPE_EXPENSE.equals(type) && !TYPE_INCOME.equals(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "type仅支持1(支出)或2(收入)");
        }
    }

    private void applyAccountBalanceChange(LedgerAccount account, Integer type, BigDecimal amount) {
        if (TYPE_INCOME.equals(type)) {
            account.setBalance(account.getBalance().add(amount));
            return;
        }
        account.setBalance(account.getBalance().subtract(amount));
    }

    private void rollbackAccountBalanceChange(LedgerAccount account, Integer type, BigDecimal amount) {
        if (TYPE_INCOME.equals(type)) {
            account.setBalance(account.getBalance().subtract(amount));
            return;
        }
        account.setBalance(account.getBalance().add(amount));
    }

    private YearMonth parseYearMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "month格式应为yyyy-MM");
        }
    }

    private LocalDate parseLocalDate(String value, String fieldName) {
        String raw = value == null ? "" : value.trim();
        try {
            if (raw.length() == 7) {
                YearMonth ym = YearMonth.parse(raw);
                // 自定义区间若传 yyyy-MM，默认按整月展开
                return fieldName.equals("startDate") ? ym.atDay(1) : ym.atEndOfMonth();
            }
            return LocalDate.parse(raw, DATE_FORMATTER);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), fieldName + "格式应为yyyy-MM-dd或yyyy-MM");
        }
    }

    private DateRange resolveRange(String month, String startDate, String endDate) {
        boolean hasStart = startDate != null && !startDate.isBlank();
        boolean hasEnd = endDate != null && !endDate.isBlank();

        if (hasStart || hasEnd) {
            if (!hasStart || !hasEnd) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "startDate和endDate需同时传入");
            }
            LocalDate start = parseLocalDate(startDate, "startDate");
            LocalDate end = parseLocalDate(endDate, "endDate");
            if (start.isAfter(end)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "startDate不能晚于endDate");
            }
            return new DateRange(start, end);
        }

        YearMonth yearMonth = parseYearMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return new DateRange(start, end);
    }

    private List<LedgerRecord> findRecordsByRange(Long userId, DateRange range) {
        return ledgerRecordRepository.findByUserIdAndRecordTimeBetweenOrderByRecordTimeAscIdAsc(
                userId,
                range.start(),
                range.endExclusive()
        );
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
        private LocalDateTime start() {
            return startDate.atStartOfDay();
        }

        private LocalDateTime endExclusive() {
            return endDate.plusDays(1).atStartOfDay();
        }
    }

    private List<LedgerCategoryStatsItemDTO> buildCategoryStats(List<LedgerRecord> records, Integer type) {
        Map<Long, LedgerCategoryStatsItemDTO> categoryMap = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LedgerRecord record : records) {
            if (!type.equals(record.getType())) {
                continue;
            }
            total = total.add(record.getAmount());
            LedgerCategoryStatsItemDTO item = categoryMap.computeIfAbsent(record.getCategoryId(), key ->
                    LedgerCategoryStatsItemDTO.builder()
                            .categoryId(record.getCategoryId())
                            .categoryName(record.getCategoryName())
                            .categoryIcon(record.getCategoryIcon())
                            .totalAmount(BigDecimal.ZERO)
                            .percentage(BigDecimal.ZERO)
                            .build()
            );
            item.setTotalAmount(item.getTotalAmount().add(record.getAmount()));
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        for (LedgerCategoryStatsItemDTO item : categoryMap.values()) {
            BigDecimal percentage = item.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP);
            item.setPercentage(percentage);
        }
        return categoryMap.values().stream()
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .toList();
    }

    private Page<LedgerRecord> queryRecordPage(
            Long userId,
            LocalDateTime start,
            LocalDateTime end,
            Long accountId,
            Long categoryId,
            Pageable pageable
    ) {
        if (accountId != null && categoryId != null) {
            return ledgerRecordRepository.findByUserIdAndRecordTimeBetweenAndAccountIdAndCategoryIdOrderByRecordTimeDescIdDesc(
                    userId,
                    start,
                    end,
                    accountId,
                    categoryId,
                    pageable
            );
        }
        if (accountId != null) {
            return ledgerRecordRepository.findByUserIdAndRecordTimeBetweenAndAccountIdOrderByRecordTimeDescIdDesc(
                    userId,
                    start,
                    end,
                    accountId,
                    pageable
            );
        }
        if (categoryId != null) {
            return ledgerRecordRepository.findByUserIdAndRecordTimeBetweenAndCategoryIdOrderByRecordTimeDescIdDesc(
                    userId,
                    start,
                    end,
                    categoryId,
                    pageable
            );
        }
        return ledgerRecordRepository.findByUserIdAndRecordTimeBetweenOrderByRecordTimeDescIdDesc(
                userId,
                start,
                end,
                pageable
        );
    }

    private LedgerAccountDTO toAccountDTO(LedgerAccount account) {
        return LedgerAccountDTO.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .icon(account.getIcon())
                .sortOrder(account.getSortOrder())
                .build();
    }

    private LedgerCategoryDTO toCategoryDTO(LedgerCategory category) {
        return LedgerCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .build();
    }

    private LedgerRecordDTO toRecordDTO(LedgerRecord record) {
        return LedgerRecordDTO.builder()
                .id(record.getId())
                .accountId(record.getAccountId())
                .categoryId(record.getCategoryId())
                .amount(record.getAmount())
                .type(record.getType())
                .recordTime(record.getRecordTime())
                .remark(record.getRemark())
                .categoryName(record.getCategoryName())
                .categoryIcon(record.getCategoryIcon())
                .accountName(record.getAccountName())
                .build();
    }
}
