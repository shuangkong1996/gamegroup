package com.gamegroup.dto;

import java.math.BigDecimal;

public class BoxDTO {

    private Long id;

    private Integer width;

    private Integer height;

    private BigDecimal totalValue;

    private BigDecimal startingPrice;

    private BigDecimal fillRate;

    private String revealedQualities;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getFillRate() {
        return fillRate;
    }

    public void setFillRate(BigDecimal fillRate) {
        this.fillRate = fillRate;
    }

    public String getRevealedQualities() {
        return revealedQualities;
    }

    public void setRevealedQualities(String revealedQualities) {
        this.revealedQualities = revealedQualities;
    }
}
