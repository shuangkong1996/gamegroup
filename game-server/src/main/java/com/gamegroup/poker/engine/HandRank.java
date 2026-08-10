package com.gamegroup.poker.engine;

/**
 * 德州扑克牌型枚举，value 越大牌型越强。
 */
public enum HandRank {

    HIGH_CARD(1),
    ONE_PAIR(2),
    TWO_PAIR(3),
    THREE_OF_A_KIND(4),
    STRAIGHT(5),
    FLUSH(6),
    FULL_HOUSE(7),
    FOUR_OF_A_KIND(8),
    STRAIGHT_FLUSH(9),
    ROYAL_FLUSH(10);

    private final int value;

    HandRank(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
