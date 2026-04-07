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
public class MortgagePrepayPlanDTO {

    /** 建议在第几年的还款节点操作（例如 5 表示满 5 年附近） */
    private Integer prepayAtYear;

    /** 对应的期数（1-based 月序） */
    private Integer monthIndex;

    /** 一次性需提前还款金额（元） */
    private BigDecimal requiredPrepayAmount;

    /** 按该方案估算的总利息（元） */
    private BigDecimal projectedTotalInterest;

    /** 相较于不提前还款可节省的利息（元） */
    private BigDecimal estimatedInterestSaved;

    /** 该窗口点是否可达目标利息 */
    private Boolean feasible;
}
