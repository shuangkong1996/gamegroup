package com.gamegroup.dto.mortgage;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MortgageLoanSliceDTO {

    private MortgageLoanType loanType;
    private BigDecimal principal;
    private BigDecimal annualRatePercentApplied;
    private BigDecimal firstMonthPayment;
    private BigDecimal lastMonthPayment;
    private BigDecimal totalPayment;
    private BigDecimal totalInterest;
    private List<MortgageScheduleRowDTO> schedule;
}
