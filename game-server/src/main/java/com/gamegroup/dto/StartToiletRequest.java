package com.gamegroup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StartToiletRequest {

    @NotNull(message = "月薪不能为空")
    @DecimalMin(value = "0.01", message = "月薪必须大于0")
    private BigDecimal monthlySalary;
}
