package com.gamegroup.dto.mortgage;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MortgageCombinedSummaryDTO {

    private BigDecimal firstMonthPayment;
    private BigDecimal lastMonthPayment;
    private BigDecimal totalPayment;
    private BigDecimal totalInterest;
}
