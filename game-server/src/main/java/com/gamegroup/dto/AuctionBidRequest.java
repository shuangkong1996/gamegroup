package com.gamegroup.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AuctionBidRequest {

    @NotNull(message = "拍卖ID不能为空")
    private Long auctionId;

    @NotNull(message = "出价金额不能为空")
    @DecimalMin(value = "0.01", message = "出价金额必须大于0")
    private BigDecimal amount;

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
