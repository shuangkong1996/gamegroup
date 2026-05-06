package com.gamegroup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateUserSettingsRequest {

    @Min(value = 0, message = "小时必须在0-23之间")
    @Max(value = 23, message = "小时必须在0-23之间")
    private Integer offWorkHour;

    @Min(value = 0, message = "分钟必须在0-59之间")
    @Max(value = 59, message = "分钟必须在0-59之间")
    private Integer offWorkMinute;

    private String city;
}
