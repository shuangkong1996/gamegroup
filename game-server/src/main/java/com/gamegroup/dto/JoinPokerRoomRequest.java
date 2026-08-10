package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinPokerRoomRequest {

    @NotBlank(message = "房号不能为空")
    private String roomNo;
}
