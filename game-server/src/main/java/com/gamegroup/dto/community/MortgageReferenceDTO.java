package com.gamegroup.dto.community;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MortgageReferenceDTO {
    private BigDecimal areaSqm;
    private BigDecimal downPaymentRatio;
    private BigDecimal totalPrice;
    private BigDecimal downPayment;
    private BigDecimal loanPrincipal;
    private BigDecimal monthlyPayment;
}
