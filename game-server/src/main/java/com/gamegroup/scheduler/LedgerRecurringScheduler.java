package com.gamegroup.scheduler;

import com.gamegroup.dto.CreateLedgerRecordRequest;
import com.gamegroup.entity.LedgerRecurringRule;
import com.gamegroup.repository.LedgerRecordRepository;
import com.gamegroup.repository.LedgerRecurringRuleRepository;
import com.gamegroup.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerRecurringScheduler {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final LedgerRecurringRuleRepository recurringRuleRepository;
    private final LedgerRecordRepository ledgerRecordRepository;
    private final LedgerService ledgerService;

    /**
     * 每天 00:05 扫描生成当月的周期账流水（避免“错过执行”）。
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void generateMonthlyRecurringRecords() {
        YearMonth nowMonth = YearMonth.now();
        String recurringMonth = nowMonth.toString(); // yyyy-MM
        LocalDateTime recordTime = nowMonth.atDay(1).atTime(9, 0); // 固定每月 1 号 09:00

        // 规则数通常很少，直接全量过滤即可；如后续变多可再优化为按状态/时间范围查询
        List<LedgerRecurringRule> rules = recurringRuleRepository.findAll();
        if (rules.isEmpty()) {
            return;
        }

        for (LedgerRecurringRule rule : rules) {
            try {
                if (rule.getStatus() == null || !STATUS_ACTIVE.equals(rule.getStatus())) {
                    continue;
                }

                if (!isRuleActiveForMonth(rule, nowMonth)) {
                    continue;
                }

                // 幂等：同一规则同一月份只生成一次
                boolean exists = ledgerRecordRepository.existsByRecurringRuleIdAndRecurringMonth(rule.getId(), recurringMonth);
                if (exists) {
                    continue;
                }

                CreateLedgerRecordRequest request = new CreateLedgerRecordRequest();
                request.setAccountId(rule.getAccountId());
                request.setCategoryId(rule.getCategoryId());
                request.setType(rule.getType());
                request.setAmount(rule.getAmount());
                request.setRecordTime(recordTime);
                request.setRemark(rule.getRemark());

                ledgerService.createRecurringRecord(rule.getUserId(), request, rule.getId(), recurringMonth);

                // 记录一下最后生成月份，便于后续排查/优化（不作为强一致条件）
                rule.setLastGeneratedMonth(recurringMonth);
                recurringRuleRepository.save(rule);
            } catch (DataIntegrityViolationException e) {
                // 兜底：如果并发/重复触发导致唯一索引冲突，则认为已生成
                log.info("Recurring record already exists (ruleId={}, month={})", rule.getId(), recurringMonth);
            } catch (Exception e) {
                log.error("Generate recurring record failed (ruleId={}, month={})", rule.getId(), recurringMonth, e);
            }
        }
    }

    private boolean isRuleActiveForMonth(LedgerRecurringRule rule, YearMonth nowMonth) {
        YearMonth start = YearMonth.parse(rule.getStartMonth());
        YearMonth end = rule.getEndMonth() == null ? null : YearMonth.parse(rule.getEndMonth());
        if (nowMonth.isBefore(start)) {
            return false;
        }
        return end == null || !nowMonth.isAfter(end);
    }
}

