package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.CreateLedgerRecurringRuleRequest;
import com.gamegroup.dto.CreateLedgerRecordRequest;
import com.gamegroup.dto.LedgerRecurringRuleDTO;
import com.gamegroup.dto.UpdateLedgerRecurringRuleStatusRequest;
import com.gamegroup.entity.LedgerAccount;
import com.gamegroup.entity.LedgerCategory;
import com.gamegroup.entity.LedgerRecurringRule;
import com.gamegroup.repository.LedgerAccountRepository;
import com.gamegroup.repository.LedgerCategoryRepository;
import com.gamegroup.repository.LedgerRecordRepository;
import com.gamegroup.repository.LedgerRecurringRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerRecurringRuleService {

    private static final Long SYSTEM_USER_ID = 0L;
    private static final Integer TYPE_EXPENSE = 1;
    private static final Integer TYPE_INCOME = 2;

    private final LedgerRecurringRuleRepository recurringRuleRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerCategoryRepository ledgerCategoryRepository;
    private final LedgerService ledgerService;
    private final LedgerRecordRepository ledgerRecordRepository;

    @Transactional
    public LedgerRecurringRuleDTO createRule(Long userId, CreateLedgerRecurringRuleRequest request) {
        validateType(request.getType());

        LedgerAccount account = ledgerAccountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_ACCOUNT_NOT_FOUND));

        LedgerCategory category = ledgerCategoryRepository
                .findByIdAndUserIdIn(request.getCategoryId(), List.of(SYSTEM_USER_ID, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.LEDGER_CATEGORY_NOT_FOUND));

        if (!request.getType().equals(category.getType())) {
            throw new BusinessException(ErrorCode.LEDGER_TYPE_MISMATCH);
        }

        YearMonth start = parseYearMonth(request.getStartMonth(), "startMonth");
        YearMonth end = null;
        if (request.getEndMonth() != null && !request.getEndMonth().isBlank()) {
            end = parseYearMonth(request.getEndMonth(), "endMonth");
        }
        if (end != null && end.isBefore(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "endMonth不能早于startMonth");
        }

        LedgerRecurringRule rule = new LedgerRecurringRule();
        rule.setUserId(userId);
        rule.setAccountId(account.getId());
        rule.setCategoryId(category.getId());
        rule.setType(request.getType());
        rule.setAmount(request.getAmount().stripTrailingZeros());
        rule.setRemark(request.getRemark());
        rule.setStartMonth(start.toString()); // yyyy-MM
        rule.setEndMonth(end == null ? null : end.toString());
        rule.setStatus("ACTIVE");

        LedgerRecurringRule saved = recurringRuleRepository.save(rule);

        // 如果规则在当前月份已生效：创建时补生成当月流水（幂等由唯一索引保证）
        YearMonth nowMonth = YearMonth.now();
        if (!nowMonth.isBefore(start) && (end == null || !nowMonth.isAfter(end))) {
            String recurringMonth = nowMonth.toString();
            boolean exists = ledgerRecordRepository.existsByRecurringRuleIdAndRecurringMonth(saved.getId(), recurringMonth);
            if (!exists) {
                CreateLedgerRecordRequest recordRequest = new CreateLedgerRecordRequest();
                recordRequest.setAccountId(saved.getAccountId());
                recordRequest.setCategoryId(saved.getCategoryId());
                recordRequest.setType(saved.getType());
                recordRequest.setAmount(saved.getAmount());
                recordRequest.setRecordTime(LocalDateTime.of(nowMonth.getYear(), nowMonth.getMonthValue(), 1, 9, 0));
                recordRequest.setRemark(saved.getRemark());
                ledgerService.createRecurringRecord(saved.getUserId(), recordRequest, saved.getId(), recurringMonth);
            }

            saved.setLastGeneratedMonth(recurringMonth);
            recurringRuleRepository.save(saved);
        }

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LedgerRecurringRuleDTO> listRules(Long userId) {
        return recurringRuleRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public LedgerRecurringRuleDTO updateStatus(Long userId, Long ruleId, UpdateLedgerRecurringRuleStatusRequest request) {
        LedgerRecurringRule rule = recurringRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "周期账规则不存在"));

        rule.setStatus(request.getStatus());
        LedgerRecurringRule saved = recurringRuleRepository.save(rule);
        return toDTO(saved);
    }

    private LedgerRecurringRuleDTO toDTO(LedgerRecurringRule rule) {
        return LedgerRecurringRuleDTO.builder()
                .id(rule.getId())
                .accountId(rule.getAccountId())
                .categoryId(rule.getCategoryId())
                .type(rule.getType())
                .amount(rule.getAmount() == null ? BigDecimal.ZERO : rule.getAmount())
                .remark(rule.getRemark())
                .startMonth(rule.getStartMonth())
                .endMonth(rule.getEndMonth())
                .status(rule.getStatus())
                .lastGeneratedMonth(rule.getLastGeneratedMonth())
                .build();
    }

    private void validateType(Integer type) {
        if (!TYPE_EXPENSE.equals(type) && !TYPE_INCOME.equals(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "type仅支持1(支出)或2(收入)");
        }
    }

    private YearMonth parseYearMonth(String month, String fieldName) {
        try {
            return YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), fieldName + "格式应为yyyy-MM");
        }
    }
}

