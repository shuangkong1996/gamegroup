package com.gamegroup.dto;

import lombok.Data;

/**
 * 游戏会话 DTO - 返回给前端的游戏状态
 */
@Data
public class GameSessionDTO {

    private Long id;
    private Long roomId;
    private Long gameId;
    private Long player1Id;
    private Long player2Id;
    private Integer currentRound;
    private Integer player1Wins;
    private Integer player2Wins;
    private String status;
    private Long winnerId;

    // 当前玩家视角的信息
    private Boolean isPlayer1;           // 当前用户是否是玩家1
    private Boolean hasChosen;           // 当前玩家是否已出招
    private Boolean opponentHasChosen;   // 对手是否已出招
    private String myChoice;             // 当前玩家的选择(仅自己可见)
    private String roundResult;          // 当前回合结果: WIN, LOSE, DRAW, null
    private String opponentChoice;       // 对手选择(仅回合结束后可见)
}
