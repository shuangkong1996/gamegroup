package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PokerHandPlayerDTO {

    private Integer seat;
    private Long userId;
    private String holeCards;
    private BigDecimal betTotal;
    private String handRank;
    private String result;
    private BigDecimal winAmount;
}
