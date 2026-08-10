package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PokerPlayerDTO {

    private Integer seat;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private BigDecimal chip;
}
