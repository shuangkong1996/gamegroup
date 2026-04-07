package com.gamegroup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLedgerRecurringRuleRequest {

    @NotNull(message = "账户ID不能为空")
    private Long accountId;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotNull(message = "类型不能为空")
    private Integer type; // 1-expense, 2-income

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于等于0.01")
    private BigDecimal amount;

    private String remark;

    @NotBlank(message = "开始月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "startMonth格式应为yyyy-MM")
    private String startMonth;

    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "endMonth格式应为yyyy-MM")
    private String endMonth; // null means forever
}

