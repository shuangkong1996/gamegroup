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
public class MortgageScheduleRowDTO {

    private int monthIndex;
    private BigDecimal monthlyPayment;
    private BigDecimal principalPortion;
    private BigDecimal interestPortion;
    private BigDecimal remainingPrincipal;
}
