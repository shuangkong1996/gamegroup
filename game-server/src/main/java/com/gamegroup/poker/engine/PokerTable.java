package com.gamegroup.poker.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 扑克房间（纯 Java）：管理座位、筹码与庄家轮转。
 * 每手牌由 {@link #startHand(Random)} 创建 {@link PokerHand}，结束后经 {@link #applyResult} 回写筹码。
 */
public final class PokerTable {

    /** 房间对外快照（不可变）。 */
    public record Seat(int seat, long playerId, BigDecimal chip) {
    }

    /** 内部可变的座位状态。 */
    private static final class Player {
        final long playerId;
        BigDecimal chip;

        Player(long playerId, BigDecimal chip) {
            this.playerId = playerId;
            this.chip = chip;
        }
    }

    private final int tableSize;
    private final BigDecimal smallBlind;
    private final BigDecimal bigBlind;
    private final Player[] seats;
    private int dealerSeat = -1;

    public PokerTable(int tableSize, BigDecimal smallBlind, BigDecimal bigBlind) {
        if (tableSize < 2 || tableSize > 9) {
            throw new IllegalArgumentException("座位数需在 2~9 之间: " + tableSize);
        }
        this.tableSize = tableSize;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.seats = new Player[tableSize];
    }

    public int tableSize() {
        return tableSize;
    }

    public BigDecimal smallBlind() {
        return smallBlind;
    }

    public BigDecimal bigBlind() {
        return bigBlind;
    }

    public int currentDealerSeat() {
        return dealerSeat;
    }

    /**
     * 入座并买入。seat 需为空、buyIn 必须为正。
     */
    public void sitDown(int seat, long playerId, BigDecimal buyIn) {
        if (seat < 0 || seat >= tableSize) {
            throw new IllegalArgumentException("非法座位: " + seat);
        }
        if (seats[seat] != null) {
            throw new IllegalStateException("座位已被占用: " + seat);
        }
        if (buyIn == null || buyIn.signum() <= 0) {
            throw new IllegalArgumentException("买入金额必须为正");
        }
        seats[seat] = new Player(playerId, buyIn);
    }

    /**
     * 离座（带走筹码由上层决定）。
     */
    public void standUp(int seat) {
        if (seat < 0 || seat >= tableSize) {
            throw new IllegalArgumentException("非法座位: " + seat);
        }
        seats[seat] = null;
    }

    public boolean canStart() {
        return activeCount() >= 2;
    }

    public int activeCount() {
        int n = 0;
        for (Player p : seats) {
            if (p != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * 开始一手新牌：轮转庄家后创建 PokerHand。
     */
    public PokerHand startHand(Random random) {
        if (!canStart()) {
            throw new IllegalStateException("玩家不足，无法开局");
        }
        if (dealerSeat < 0) {
            dealerSeat = firstOccupiedSeat();
        } else {
            dealerSeat = nextOccupiedSeatFrom(dealerSeat);
        }
        return new PokerHand(this, random);
    }

    /**
     * 应用一手牌的结算结果：回写筹码并移除破产玩家。
     */
    public void applyResult(PokerHand.HandResult result) {
        result.chipDelta().forEach((seat, delta) -> {
            Player p = seats[seat];
            if (p != null) {
                p.chip = p.chip.add(delta);
            }
        });
        for (int i = 0; i < tableSize; i++) {
            if (seats[i] != null && seats[i].chip.signum() <= 0) {
                seats[i] = null;
            }
        }
    }

    public List<Seat> snapshot() {
        List<Seat> list = new ArrayList<>();
        for (int i = 0; i < tableSize; i++) {
            if (seats[i] != null) {
                list.add(new Seat(i, seats[i].playerId, seats[i].chip));
            }
        }
        return list;
    }

    public boolean isOccupied(int seat) {
        return seat >= 0 && seat < tableSize && seats[seat] != null;
    }

    private int firstOccupiedSeat() {
        for (int i = 0; i < tableSize; i++) {
            if (seats[i] != null) {
                return i;
            }
        }
        throw new IllegalStateException("无玩家入座");
    }

    private int nextOccupiedSeatFrom(int seat) {
        for (int i = 1; i <= tableSize; i++) {
            int s = (seat + i) % tableSize;
            if (seats[s] != null) {
                return s;
            }
        }
        throw new IllegalStateException("无玩家入座");
    }
}
