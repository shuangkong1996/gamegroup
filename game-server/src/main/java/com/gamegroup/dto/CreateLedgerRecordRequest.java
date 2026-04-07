package com.gamegroup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateLedgerRecordRequest {

    @NotNull(message = "账户ID不能为空")
    private Long accountId;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "类型不能为空")
    private Integer type;

    @NotNull(message = "记账时间不能为空")
    private LocalDateTime recordTime;

    private String remark;
}
