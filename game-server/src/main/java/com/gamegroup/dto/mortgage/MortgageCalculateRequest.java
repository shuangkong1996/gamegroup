package com.gamegroup.dto.mortgage;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MortgageCalculateRequest {

    @NotNull
    @Min(1)
    @Max(40)
    private Integer years;

    @NotNull private MortgageRepaymentMethod repaymentMethod;

    @NotNull
    @DecimalMin("0")
    private BigDecimal commercialPrincipal;

    @NotNull
    @DecimalMin("0")
    private BigDecimal providentFundPrincipal;

    /** 为空则使用服务端配置的商贷默认年利率（%） */
    @DecimalMin("0")
    @DecimalMax("50")
    private BigDecimal commercialAnnualRatePercent;

    /** 为空则使用服务端配置的公积金默认年利率（%） */
    @DecimalMin("0")
    @DecimalMax("50")
    private BigDecimal providentFundAnnualRatePercent;

    /**
     * 公积金首套/二套；为空按首套。仅在未传 {@link #providentFundAnnualRatePercent} 时用于按年限匹配人行利率档位。
     */
    private MortgageProvidentTier providentFundTier;

    /** 目标总利息金额（元），用于给出提前还款建议；为空表示不计算建议 */
    @DecimalMin("0")
    private BigDecimal targetTotalInterestAmount;

    /** 提前还款建议窗口起始年（含），默认 5 */
    @Min(1)
    @Max(40)
    private Integer prepayWindowStartYear;

    /** 提前还款建议窗口结束年（含），默认 10 */
    @Min(1)
    @Max(40)
    private Integer prepayWindowEndYear;
}
