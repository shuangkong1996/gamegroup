package com.gamegroup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRoomRequest {

    @NotNull(message = "游戏ID不能为空")
    private Long gameId;
}
