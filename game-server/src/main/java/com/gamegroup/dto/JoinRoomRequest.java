package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinRoomRequest {

    @NotBlank(message = "验证号不能为空")
    private String verifyCode;
}
