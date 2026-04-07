package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LedgerRecurringRuleDTO {

    private Long id;
    private Long accountId;
    private Long categoryId;
    private Integer type;
    private BigDecimal amount;
    private String remark;

    private String startMonth; // yyyy-MM
    private String endMonth; // yyyy-MM, null means forever

    private String status; // ACTIVE/PAUSED
    private String lastGeneratedMonth; // yyyy-MM
}

