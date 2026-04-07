package com.gamegroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionDTO {

    private Long id;

    private Long roomId;

    private Long boxId;

    private Integer currentRound;

    private String status;

    private Long winnerId;

    private BigDecimal finalPrice;

    private LocalDateTime roundEndTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public LocalDateTime getRoundEndTime() {
        return roundEndTime;
    }

    public void setRoundEndTime(LocalDateTime roundEndTime) {
        this.roundEndTime = roundEndTime;
    }
}
