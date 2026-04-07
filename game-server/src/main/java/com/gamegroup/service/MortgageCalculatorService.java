package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.config.MortgageDefaultsProperties;
import com.gamegroup.dto.mortgage.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MortgageCalculatorService {

    private static final MathContext MC = new MathContext(14, RoundingMode.HALF_UP);
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private final MortgageDefaultsProperties defaultsProperties;

    public MortgageDefaultsResponse getDefaults() {
        return MortgageDefaultsResponse.builder()
                .commercialAnnualRatePercent(defaultsProperties.getCommercialAnnualRatePercent())
                .providentFundFirstHomeFiveYearOrBelowPercent(
                        defaultsProperties.getProvidentFundFirstHomeFiveYearOrBelowPercent())
                .providentFundFirstHomeFiveYearPlusPercent(
                        defaultsProperties.getProvidentFundFirstHomeFiveYearPlusPercent())
                .providentFundSecondHomeFiveYearOrBelowPercent(
                        defaultsProperties.getProvidentFundSecondHomeFiveYearOrBelowPercent())
                .providentFundSecondHomeFiveYearPlusPercent(
                        defaultsProperties.getProvidentFundSecondHomeFiveYearPlusPercent())
                .providentFundRateNote(defaultsProperties.getProvidentFundRateNote())
                .referencePeriod(defaultsProperties.getReferencePeriod())
                .referenceNote(defaultsProperties.getReferenceNote())
                .build();
    }

    public MortgageCalculateResponse calculate(MortgageCalculateRequest request) {
        BigDecimal comPrincipal = nullToZero(request.getCommercialPrincipal());
        BigDecimal fundPrincipal = nullToZero(request.getProvidentFundPrincipal());
        if (comPrincipal.signum() <= 0 && fundPrincipal.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请至少填写商贷或公积金贷款本金");
        }

        int months = request.getYears() * 12;
        BigDecimal comRate = effectiveRate(
                request.getCommercialAnnualRatePercent(), defaultsProperties.getCommercialAnnualRatePercent());
        MortgageProvidentTier tier =
                request.getProvidentFundTier() != null ? request.getProvidentFundTier() : MortgageProvidentTier.FIRST_HOME;
        BigDecimal fundDefault = defaultProvidentAnnualRatePercent(tier, request.getYears());
        BigDecimal fundRate = effectiveRate(request.getProvidentFundAnnualRatePercent(), fundDefault);

        MortgageLoanSliceDTO commercial =
                buildSlice(MortgageLoanType.COMMERCIAL, comPrincipal, comRate, months, request.getRepaymentMethod());
        MortgageLoanSliceDTO provident = buildSlice(
                MortgageLoanType.PROVIDENT_FUND, fundPrincipal, fundRate, months, request.getRepaymentMethod());

        List<MortgageLoanSliceDTO> loans = List.of(commercial, provident);
        MortgageCombinedSummaryDTO combined = combineSummary(loans, months);
        List<MortgageScheduleRowDTO> combinedSchedule = mergeSchedules(commercial, provident, months);
        MortgagePrepayAdviceDTO prepayAdvice = buildPrepayAdvice(request, loans, combined, months, request.getRepaymentMethod());

        return MortgageCalculateResponse.builder()
                .combined(combined)
                .loans(loans)
                .combinedSchedule(combinedSchedule)
                .prepayAdvice(prepayAdvice)
                .build();
    }

    private MortgagePrepayAdviceDTO buildPrepayAdvice(
            MortgageCalculateRequest request,
            List<MortgageLoanSliceDTO> loans,
            MortgageCombinedSummaryDTO combined,
            int totalMonths,
            MortgageRepaymentMethod repaymentMethod) {
        BigDecimal target = request.getTargetTotalInterestAmount();
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        int startYear = request.getPrepayWindowStartYear() == null ? 5 : request.getPrepayWindowStartYear();
        int endYear = request.getPrepayWindowEndYear() == null ? 10 : request.getPrepayWindowEndYear();
        if (startYear > endYear) {
            int tmp = startYear;
            startYear = endYear;
            endYear = tmp;
        }

        int minMonth = Math.max(1, startYear * 12);
        int maxMonth = Math.min(totalMonths - 1, endYear * 12);
        if (minMonth > maxMonth) {
            return MortgagePrepayAdviceDTO.builder()
                    .targetTotalInterestAmount(scaleMoney(target))
                    .windowStartYear(startYear)
                    .windowEndYear(endYear)
                    .baseTotalInterest(scaleMoney(combined.getTotalInterest()))
                    .optimalPlan(null)
                    .plans(Collections.emptyList())
                    .build();
        }

        List<MortgagePrepayPlanDTO> plans = new ArrayList<>();
        for (int month = minMonth; month <= maxMonth; month += 12) {
            plans.add(buildPlanForMonth(month, loans, target, combined.getTotalInterest(), repaymentMethod));
        }

        plans.sort(Comparator.comparing(MortgagePrepayPlanDTO::getRequiredPrepayAmount));
        MortgagePrepayPlanDTO optimalPlan = plans.stream()
                .filter(MortgagePrepayPlanDTO::getFeasible)
                .findFirst()
                .orElse(null);
        return MortgagePrepayAdviceDTO.builder()
                .targetTotalInterestAmount(scaleMoney(target))
                .windowStartYear(startYear)
                .windowEndYear(endYear)
                .baseTotalInterest(scaleMoney(combined.getTotalInterest()))
                .optimalPlan(optimalPlan)
                .plans(plans)
                .build();
    }

    private MortgagePrepayPlanDTO buildPlanForMonth(
            int month,
            List<MortgageLoanSliceDTO> loans,
            BigDecimal targetInterest,
            BigDecimal baseTotalInterest,
            MortgageRepaymentMethod repaymentMethod) {
        BigDecimal paidInterest = BigDecimal.ZERO;
        BigDecimal remainingPrincipalTotal = BigDecimal.ZERO;
        List<LoanRemainState> states = new ArrayList<>();

        for (MortgageLoanSliceDTO loan : loans) {
            if (loan.getPrincipal().compareTo(BigDecimal.ZERO) <= 0 || loan.getSchedule().isEmpty()) {
                continue;
            }
            List<MortgageScheduleRowDTO> s = loan.getSchedule();
            int cutoff = Math.min(month, s.size());
            BigDecimal interestSoFar = sumInterest(s, cutoff);
            paidInterest = paidInterest.add(interestSoFar);
            BigDecimal remaining = s.get(cutoff - 1).getRemainingPrincipal();
            remainingPrincipalTotal = remainingPrincipalTotal.add(remaining);
            states.add(new LoanRemainState(loan, remaining, s.size() - cutoff));
        }

        if (states.isEmpty()) {
            return plan(month, BigDecimal.ZERO, paidInterest, baseTotalInterest, paidInterest.compareTo(targetInterest) <= 0);
        }

        // 目标利息低于「该月之前已付利息」时，不可能通过此后提前还款达成。
        if (paidInterest.compareTo(targetInterest) > 0) {
            return plan(month, remainingPrincipalTotal, paidInterest, baseTotalInterest, false);
        }

        BigDecimal minProjected = projectedInterestAfterPrepay(states, remainingPrincipalTotal, paidInterest, repaymentMethod);
        if (minProjected.compareTo(targetInterest) > 0) {
            return plan(month, remainingPrincipalTotal, minProjected, baseTotalInterest, false);
        }

        BigDecimal low = BigDecimal.ZERO;
        BigDecimal high = remainingPrincipalTotal;
        for (int i = 0; i < 60; i++) {
            BigDecimal mid = low.add(high).divide(BigDecimal.valueOf(2), MONEY_SCALE + 6, RM);
            BigDecimal projected = projectedInterestAfterPrepay(states, mid, paidInterest, repaymentMethod);
            if (projected.compareTo(targetInterest) <= 0) {
                high = mid;
            } else {
                low = mid;
            }
        }
        BigDecimal required = high.setScale(MONEY_SCALE, RM);
        BigDecimal projected = projectedInterestAfterPrepay(states, required, paidInterest, repaymentMethod);
        return plan(month, required, projected, baseTotalInterest, true);
    }

    private MortgagePrepayPlanDTO plan(
            int month, BigDecimal prepay, BigDecimal projectedInterest, BigDecimal baseTotalInterest, boolean feasible) {
        return MortgagePrepayPlanDTO.builder()
                .prepayAtYear((month + 11) / 12)
                .monthIndex(month)
                .requiredPrepayAmount(scaleMoney(prepay))
                .projectedTotalInterest(scaleMoney(projectedInterest))
                .estimatedInterestSaved(scaleMoney(baseTotalInterest.subtract(projectedInterest)))
                .feasible(feasible)
                .build();
    }

    private BigDecimal projectedInterestAfterPrepay(
            List<LoanRemainState> states,
            BigDecimal prepayAmount,
            BigDecimal paidInterestSoFar,
            MortgageRepaymentMethod repaymentMethod) {
        BigDecimal remainToAllocate = prepayAmount;
        List<LoanRemainState> sorted = new ArrayList<>(states);
        sorted.sort((a, b) ->
                b.loan().getAnnualRatePercentApplied().compareTo(a.loan().getAnnualRatePercentApplied()));

        BigDecimal futureInterest = BigDecimal.ZERO;
        for (LoanRemainState st : sorted) {
            BigDecimal allocate = remainToAllocate.min(st.remainingPrincipal());
            remainToAllocate = remainToAllocate.subtract(allocate);
            BigDecimal newPrincipal = st.remainingPrincipal().subtract(allocate);
            if (newPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                newPrincipal = BigDecimal.ZERO;
            }
            futureInterest =
                    futureInterest.add(simulateFutureInterest(st.loan(), newPrincipal, st.remainingMonths(), repaymentMethod));
        }
        return paidInterestSoFar.add(futureInterest);
    }

    private BigDecimal simulateFutureInterest(
            MortgageLoanSliceDTO loan,
            BigDecimal remainingPrincipal,
            int remainingMonths,
            MortgageRepaymentMethod repaymentMethod) {
        if (remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0 || remainingMonths <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RM);
        }
        BigDecimal monthlyRate = toMonthlyRate(loan.getAnnualRatePercentApplied());
        List<MortgageScheduleRowDTO> future = repaymentMethod == MortgageRepaymentMethod.EQUAL_INSTALLMENT
                ? scheduleEqualInstallment(remainingPrincipal, monthlyRate, remainingMonths)
                : scheduleEqualPrincipal(remainingPrincipal, monthlyRate, remainingMonths);
        BigDecimal interest = BigDecimal.ZERO;
        for (MortgageScheduleRowDTO row : future) {
            interest = interest.add(row.getInterestPortion());
        }
        return scaleMoney(interest);
    }

    private BigDecimal sumInterest(List<MortgageScheduleRowDTO> schedule, int count) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < count; i++) {
            sum = sum.add(schedule.get(i).getInterestPortion());
        }
        return sum;
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        return v.setScale(MONEY_SCALE, RM);
    }

    private record LoanRemainState(MortgageLoanSliceDTO loan, BigDecimal remainingPrincipal, int remainingMonths) {}

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal effectiveRate(BigDecimal requestRate, BigDecimal configuredDefault) {
        if (requestRate != null && requestRate.compareTo(BigDecimal.ZERO) > 0) {
            return requestRate;
        }
        return configuredDefault;
    }

    /** 贷款年限大于 5 年适用「5 年以上」利率档，否则「5 年及以下」档 */
    private BigDecimal defaultProvidentAnnualRatePercent(MortgageProvidentTier tier, int years) {
        boolean fiveYearPlus = years > 5;
        if (tier == MortgageProvidentTier.SECOND_HOME) {
            return fiveYearPlus
                    ? defaultsProperties.getProvidentFundSecondHomeFiveYearPlusPercent()
                    : defaultsProperties.getProvidentFundSecondHomeFiveYearOrBelowPercent();
        }
        return fiveYearPlus
                ? defaultsProperties.getProvidentFundFirstHomeFiveYearPlusPercent()
                : defaultsProperties.getProvidentFundFirstHomeFiveYearOrBelowPercent();
    }

    private MortgageLoanSliceDTO buildSlice(
            MortgageLoanType type,
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int months,
            MortgageRepaymentMethod method) {
        if (principal.signum() <= 0) {
            return MortgageLoanSliceDTO.builder()
                    .loanType(type)
                    .principal(BigDecimal.ZERO.setScale(MONEY_SCALE, RM))
                    .annualRatePercentApplied(annualRatePercent.setScale(MONEY_SCALE, RM))
                    .firstMonthPayment(BigDecimal.ZERO.setScale(MONEY_SCALE, RM))
                    .lastMonthPayment(BigDecimal.ZERO.setScale(MONEY_SCALE, RM))
                    .totalPayment(BigDecimal.ZERO.setScale(MONEY_SCALE, RM))
                    .totalInterest(BigDecimal.ZERO.setScale(MONEY_SCALE, RM))
                    .schedule(Collections.emptyList())
                    .build();
        }

        BigDecimal monthlyRate = toMonthlyRate(annualRatePercent);
        List<MortgageScheduleRowDTO> schedule =
                method == MortgageRepaymentMethod.EQUAL_INSTALLMENT
                        ? scheduleEqualInstallment(principal, monthlyRate, months)
                        : scheduleEqualPrincipal(principal, monthlyRate, months);

        BigDecimal totalPay = BigDecimal.ZERO;
        BigDecimal totalInt = BigDecimal.ZERO;
        for (MortgageScheduleRowDTO row : schedule) {
            totalPay = totalPay.add(row.getMonthlyPayment());
            totalInt = totalInt.add(row.getInterestPortion());
        }

        return MortgageLoanSliceDTO.builder()
                .loanType(type)
                .principal(principal.setScale(MONEY_SCALE, RM))
                .annualRatePercentApplied(annualRatePercent.setScale(MONEY_SCALE, RM))
                .firstMonthPayment(schedule.get(0).getMonthlyPayment())
                .lastMonthPayment(schedule.get(schedule.size() - 1).getMonthlyPayment())
                .totalPayment(totalPay.setScale(MONEY_SCALE, RM))
                .totalInterest(totalInt.setScale(MONEY_SCALE, RM))
                .schedule(schedule)
                .build();
    }

    private static BigDecimal toMonthlyRate(BigDecimal annualPercent) {
        return annualPercent
                .divide(BigDecimal.valueOf(12), 12, RM)
                .divide(BigDecimal.valueOf(100), 12, RM);
    }

    private List<MortgageScheduleRowDTO> scheduleEqualInstallment(
            BigDecimal principal, BigDecimal monthlyRate, int n) {
        List<MortgageScheduleRowDTO> rows = new ArrayList<>(n);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal m = principal.divide(BigDecimal.valueOf(n), MONEY_SCALE, RM);
            BigDecimal balance = principal.setScale(MONEY_SCALE, RM);
            for (int i = 1; i <= n; i++) {
                BigDecimal prin = (i == n) ? balance : m;
                BigDecimal interest = BigDecimal.ZERO.setScale(MONEY_SCALE, RM);
                balance = balance.subtract(prin).setScale(MONEY_SCALE, RM);
                rows.add(MortgageScheduleRowDTO.builder()
                        .monthIndex(i)
                        .monthlyPayment(prin.add(interest))
                        .principalPortion(prin)
                        .interestPortion(interest)
                        .remainingPrincipal(balance.max(BigDecimal.ZERO))
                        .build());
            }
            return rows;
        }

        BigDecimal monthly = monthlyInstallmentPayment(principal, monthlyRate, n);
        BigDecimal balance = principal.setScale(MONEY_SCALE, RM);
        for (int month = 1; month <= n; month++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(MONEY_SCALE, RM);
            BigDecimal principalPaid;
            if (month < n) {
                principalPaid = monthly.subtract(interest);
                if (principalPaid.compareTo(balance) > 0) {
                    principalPaid = balance;
                }
            } else {
                principalPaid = balance;
            }
            BigDecimal payment = principalPaid.add(interest).setScale(MONEY_SCALE, RM);
            balance = balance.subtract(principalPaid).setScale(MONEY_SCALE, RM);
            if (balance.signum() < 0) {
                balance = BigDecimal.ZERO.setScale(MONEY_SCALE, RM);
            }
            rows.add(MortgageScheduleRowDTO.builder()
                    .monthIndex(month)
                    .monthlyPayment(payment)
                    .principalPortion(principalPaid.setScale(MONEY_SCALE, RM))
                    .interestPortion(interest)
                    .remainingPrincipal(balance)
                    .build());
        }
        return rows;
    }

    private static BigDecimal monthlyInstallmentPayment(BigDecimal p, BigDecimal r, int n) {
        BigDecimal one = BigDecimal.ONE;
        BigDecimal pow = one.add(r).pow(n, MC);
        BigDecimal num = p.multiply(r).multiply(pow);
        BigDecimal den = pow.subtract(one);
        if (den.signum() == 0) {
            return p.divide(BigDecimal.valueOf(n), MONEY_SCALE, RM);
        }
        return num.divide(den, MONEY_SCALE, RM);
    }

    private List<MortgageScheduleRowDTO> scheduleEqualPrincipal(
            BigDecimal principal, BigDecimal monthlyRate, int n) {
        List<MortgageScheduleRowDTO> rows = new ArrayList<>(n);
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(n), MONEY_SCALE, RM);
        BigDecimal balance = principal.setScale(MONEY_SCALE, RM);
        for (int month = 1; month <= n; month++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(MONEY_SCALE, RM);
            BigDecimal principalPaid = month == n ? balance : monthlyPrincipal;
            BigDecimal payment = principalPaid.add(interest).setScale(MONEY_SCALE, RM);
            balance = balance.subtract(principalPaid).setScale(MONEY_SCALE, RM);
            if (balance.signum() < 0) {
                balance = BigDecimal.ZERO.setScale(MONEY_SCALE, RM);
            }
            rows.add(MortgageScheduleRowDTO.builder()
                    .monthIndex(month)
                    .monthlyPayment(payment)
                    .principalPortion(principalPaid.setScale(MONEY_SCALE, RM))
                    .interestPortion(interest)
                    .remainingPrincipal(balance)
                    .build());
        }
        return rows;
    }

    private MortgageCombinedSummaryDTO combineSummary(List<MortgageLoanSliceDTO> loans, int months) {
        BigDecimal first = BigDecimal.ZERO;
        BigDecimal last = BigDecimal.ZERO;
        BigDecimal totalPay = BigDecimal.ZERO;
        BigDecimal totalInt = BigDecimal.ZERO;
        for (MortgageLoanSliceDTO loan : loans) {
            if (loan.getSchedule().isEmpty()) {
                continue;
            }
            first = first.add(loan.getFirstMonthPayment());
            last = last.add(loan.getLastMonthPayment());
            totalPay = totalPay.add(loan.getTotalPayment());
            totalInt = totalInt.add(loan.getTotalInterest());
        }
        return MortgageCombinedSummaryDTO.builder()
                .firstMonthPayment(first.setScale(MONEY_SCALE, RM))
                .lastMonthPayment(last.setScale(MONEY_SCALE, RM))
                .totalPayment(totalPay.setScale(MONEY_SCALE, RM))
                .totalInterest(totalInt.setScale(MONEY_SCALE, RM))
                .build();
    }

    private List<MortgageScheduleRowDTO> mergeSchedules(
            MortgageLoanSliceDTO a, MortgageLoanSliceDTO b, int months) {
        List<MortgageScheduleRowDTO> sa = a.getSchedule();
        List<MortgageScheduleRowDTO> sb = b.getSchedule();
        if (sa.isEmpty() && sb.isEmpty()) {
            return Collections.emptyList();
        }
        List<MortgageScheduleRowDTO> out = new ArrayList<>(months);
        for (int i = 0; i < months; i++) {
            MortgageScheduleRowDTO ra = i < sa.size() ? sa.get(i) : zeroRow(i + 1);
            MortgageScheduleRowDTO rb = i < sb.size() ? sb.get(i) : zeroRow(i + 1);
            BigDecimal pay = ra.getMonthlyPayment().add(rb.getMonthlyPayment());
            BigDecimal prin = ra.getPrincipalPortion().add(rb.getPrincipalPortion());
            BigDecimal intr = ra.getInterestPortion().add(rb.getInterestPortion());
            BigDecimal rem = ra.getRemainingPrincipal().add(rb.getRemainingPrincipal());
            out.add(MortgageScheduleRowDTO.builder()
                    .monthIndex(i + 1)
                    .monthlyPayment(pay.setScale(MONEY_SCALE, RM))
                    .principalPortion(prin.setScale(MONEY_SCALE, RM))
                    .interestPortion(intr.setScale(MONEY_SCALE, RM))
                    .remainingPrincipal(rem.setScale(MONEY_SCALE, RM))
                    .build());
        }
        return out;
    }

    private static MortgageScheduleRowDTO zeroRow(int monthIndex) {
        BigDecimal z = BigDecimal.ZERO.setScale(MONEY_SCALE, RM);
        return MortgageScheduleRowDTO.builder()
                .monthIndex(monthIndex)
                .monthlyPayment(z)
                .principalPortion(z)
                .interestPortion(z)
                .remainingPrincipal(z)
                .build();
    }
}
