package com.gamegroup.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mortgage.defaults")
public class MortgageDefaultsProperties {

    /** 商贷默认年利率（%） */
    private BigDecimal commercialAnnualRatePercent = new BigDecimal("3.05");

    /** 公积金首套：5 年及以下（含）年利率（%），人行公示 */
    private BigDecimal providentFundFirstHomeFiveYearOrBelowPercent = new BigDecimal("2.10");

    /** 公积金首套：5 年以上年利率（%） */
    private BigDecimal providentFundFirstHomeFiveYearPlusPercent = new BigDecimal("2.60");

    /** 公积金二套：5 年及以下（含）年利率（%） */
    private BigDecimal providentFundSecondHomeFiveYearOrBelowPercent = new BigDecimal("2.525");

    /** 公积金二套：5 年以上年利率（%） */
    private BigDecimal providentFundSecondHomeFiveYearPlusPercent = new BigDecimal("3.075");

    /** 参考时间说明 */
    private String referencePeriod = "2026-03";

    /** 公积金分档利率说明 */
    private String providentFundRateNote = "";

    /** 给用户看的免责说明 */
    private String referenceNote = "";
}
