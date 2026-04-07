package com.gamegroup.dto;

import jakarta.validation.constraints.NotNull;

public class StartAuctionRequest {

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
