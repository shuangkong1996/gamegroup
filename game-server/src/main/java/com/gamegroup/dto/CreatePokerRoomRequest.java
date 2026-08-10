package com.gamegroup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePokerRoomRequest {

    @NotBlank(message = "房间名不能为空")
    private String name;

    @NotNull(message = "座位数不能为空")
    @Min(value = 2, message = "至少 2 人")
    @Max(value = 9, message = "最多 9 人")
    private Integer tableSize = 6;

    @NotNull(message = "小盲不能为空")
    @DecimalMin(value = "0.5", message = "小盲不能小于 0.5")
    private BigDecimal smallBlind;

    @NotNull(message = "大盲不能为空")
    @DecimalMin(value = "1", message = "大盲不能小于 1")
    private BigDecimal bigBlind;

    @NotNull(message = "最小买入不能为空")
    @DecimalMin(value = "1", message = "最小买入不能小于 1")
    private BigDecimal buyInMin;

    @NotNull(message = "最大买入不能为空")
    @DecimalMin(value = "1", message = "最大买入不能小于 1")
    private BigDecimal buyInMax;

    @Min(value = 5, message = "思考时间不能少于 5 秒")
    @Max(value = 120, message = "思考时间不能超过 120 秒")
    private Integer thinkSeconds = 20;
}
