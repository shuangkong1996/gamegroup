package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PokerStatsDTO {

    private Long userId;

    /** 参与手数 */
    private Integer handsPlayed;

    /** 赢下手数 */
    private Integer handsWon;

    /** 胜率（0~1） */
    private BigDecimal winRate;

    /** 累计赢输（正为赢，负为输） */
    private BigDecimal totalWin;

    /** 累计投入 */
    private BigDecimal totalBet;
}
