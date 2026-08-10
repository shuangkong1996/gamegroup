package com.gamegroup.poker.engine;

import java.util.Arrays;

/**
 * 一手 5 张牌（或组合后的最优 5 张）的评估结果。
 * tieBreakers 为按重要性降序排列的关键牌 rank，用于同牌型间的胜负比较。
 */
public record HandValue(HandRank rank, int[] tieBreakers) implements Comparable<HandValue> {

    public HandValue {
        tieBreakers = tieBreakers.clone();
    }

    @Override
    public int compareTo(HandValue other) {
        int c = Integer.compare(rank.value(), other.rank.value());
        if (c != 0) {
            return c;
        }
        int n = Math.min(tieBreakers.length, other.tieBreakers.length);
        for (int i = 0; i < n; i++) {
            c = Integer.compare(tieBreakers[i], other.tieBreakers[i]);
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(tieBreakers.length, other.tieBreakers.length);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HandValue other
                && rank == other.rank
                && Arrays.equals(tieBreakers, other.tieBreakers);
    }

    @Override
    public int hashCode() {
        return 31 * rank.hashCode() + Arrays.hashCode(tieBreakers);
    }

    @Override
    public String toString() {
        return rank + " " + Arrays.toString(tieBreakers);
    }
}
