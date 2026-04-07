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
public class MortgagePrepayAdviceDTO {

    private BigDecimal targetTotalInterestAmount;
    private Integer windowStartYear;
    private Integer windowEndYear;
    private BigDecimal baseTotalInterest;
    /** 推荐最优节点：优先可达成目标且所需提前还款最少 */
    private MortgagePrepayPlanDTO optimalPlan;
    private List<MortgagePrepayPlanDTO> plans;
}
