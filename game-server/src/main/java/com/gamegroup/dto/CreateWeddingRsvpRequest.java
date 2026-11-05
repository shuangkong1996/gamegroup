package com.gamegroup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWeddingRsvpRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 40, message = "姓名不能超过40个字符")
    private String name;

    @NotNull(message = "请填写是否出席")
    private Boolean attending;

    @NotNull(message = "请填写出席人数")
    @Min(value = 0, message = "出席人数不能小于0")
    @Max(value = 6, message = "出席人数不能超过6")
    private Integer guestCount;
}
