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
public class MortgageDefaultsResponse {

    private BigDecimal commercialAnnualRatePercent;

    private BigDecimal providentFundFirstHomeFiveYearOrBelowPercent;
    private BigDecimal providentFundFirstHomeFiveYearPlusPercent;
    private BigDecimal providentFundSecondHomeFiveYearOrBelowPercent;
    private BigDecimal providentFundSecondHomeFiveYearPlusPercent;

    /** 公积金利率档位说明（人行公示，随政策调整） */
    private String providentFundRateNote;

    private String referencePeriod;
    private String referenceNote;
}
