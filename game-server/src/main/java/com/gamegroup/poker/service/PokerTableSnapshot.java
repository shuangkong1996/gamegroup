package com.gamegroup.poker.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 牌桌快照（WS TABLE_SNAPSHOT 的 data）。每个玩家收到的快照中，holeCards 仅包含本人底牌。
 */
@Data
@Builder
public class PokerTableSnapshot {

    private Long roomId;
    private String roomNo;
    private String name;
    private Integer tableSize;
    private BigDecimal smallBlind;
    private BigDecimal bigBlind;
    private String status;
    private Integer handNo;
    private Integer dealerSeat;
    private String phase;
    private List<String> board;
    private BigDecimal pot;
    private BigDecimal currentBet;
    private BigDecimal minRaise;
    private Integer currentActor;
    private BigDecimal callAmount;
    private Long actionDeadline;
    private Boolean isOwner;
    private Integer mySeat;
    private List<PlayerView> players;
    private HandResultView lastHand;

    @Data
    @Builder
    public static class PlayerView {
        private Integer seat;
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private BigDecimal chip;
        private List<String> holeCards;
        private BigDecimal roundBet;
        private BigDecimal totalBet;
        private Boolean folded;
        private Boolean allIn;
        private Boolean isDealer;
    }

    @Data
    @Builder
    public static class HandResultView {
        private BigDecimal pot;
        private List<Integer> winners;
        private Map<Integer, BigDecimal> chipDelta;
        private Map<Integer, List<String>> revealedHoleCards;
        private Map<Integer, String> bestHands;
    }
}
