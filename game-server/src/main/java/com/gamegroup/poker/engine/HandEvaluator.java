package com.gamegroup.poker.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 牌型评估器（纯 Java）：从最多 7 张牌中选出最优 5 张组合并判定牌型。
 * 采用 5 张组合暴力枚举（C(7,5)=21 种），实现简单且不易出错。
 */
public final class HandEvaluator {

    private HandEvaluator() {
    }

    /**
     * 评估结果：牌型价值 + 组成该牌型的最佳 5 张牌（按点数降序，便于展示）。
     */
    public record EvaluatedHand(HandValue value, List<Card> bestFive) {
    }

    /**
     * 评估 5~7 张牌，返回最优的 5 张组合的牌型结果。
     */
    public static HandValue evaluate(List<Card> cards) {
        return evaluateWithBestFive(cards).value();
    }

    /**
     * 评估 5~7 张牌，返回牌型结果及组成该牌型的最佳 5 张牌（按点数降序）。
     */
    public static EvaluatedHand evaluateWithBestFive(List<Card> cards) {
        if (cards == null || cards.size() < 5 || cards.size() > 7) {
            throw new IllegalArgumentException("评估需要 5~7 张牌，实际: "
                    + (cards == null ? "null" : cards.size()));
        }
        HandValue best = null;
        Card[] bestFive = null;
        int n = cards.size();
        for (int a = 0; a < n - 4; a++) {
            for (int b = a + 1; b < n - 3; b++) {
                for (int c = b + 1; c < n - 2; c++) {
                    for (int d = c + 1; d < n - 1; d++) {
                        for (int e = d + 1; e < n; e++) {
                            Card[] five = {cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e)};
                            HandValue value = evaluateFive(five[0], five[1], five[2], five[3], five[4]);
                            if (best == null || value.compareTo(best) > 0) {
                                best = value;
                                bestFive = five;
                            }
                        }
                    }
                }
            }
        }
        List<Card> sorted = Arrays.stream(bestFive)
                .sorted((x, y) -> Integer.compare(y.rank(), x.rank()))
                .toList();
        return new EvaluatedHand(best, sorted);
    }

    /**
     * 评估恰好 5 张牌。
     */
    public static HandValue evaluateFive(Card c1, Card c2, Card c3, Card c4, Card c5) {
        Card[] five = {c1, c2, c3, c4, c5};
        int[] counts = new int[15];
        for (Card c : five) {
            counts[c.rank()]++;
        }

        boolean isFlush = true;
        char suit0 = five[0].suit();
        for (Card c : five) {
            if (c.suit() != suit0) {
                isFlush = false;
                break;
            }
        }

        // 按 (张数降序, 点数降序) 排列的 rank 分组
        List<int[]> groups = new ArrayList<>(5);
        for (int r = Card.MAX_RANK; r >= Card.MIN_RANK; r--) {
            if (counts[r] > 0) {
                groups.add(new int[]{r, counts[r]});
            }
        }
        groups.sort((x, y) -> {
            int c = Integer.compare(y[1], x[1]);
            return c != 0 ? c : Integer.compare(y[0], x[0]);
        });
        int size = groups.size();
        int[] ranksArr = new int[size];
        int[] countArr = new int[size];
        for (int i = 0; i < size; i++) {
            ranksArr[i] = groups.get(i)[0];
            countArr[i] = groups.get(i)[1];
        }

        // 顺子判定：5 张不重复且连续（含 A2345 轮子顺）
        boolean isStraight = false;
        int straightHigh = 0;
        if (size == 5) {
            if (ranksArr[0] - ranksArr[4] == 4) {
                isStraight = true;
                straightHigh = ranksArr[0];
            } else if (ranksArr[0] == Card.MAX_RANK && ranksArr[1] == 5 && ranksArr[4] == 2) {
                isStraight = true;
                straightHigh = 5;
            }
        }

        if (size == 2) {
            if (countArr[0] == 4) {
                // 四条 [四条rank, 单张kicker]
                return new HandValue(HandRank.FOUR_OF_A_KIND, new int[]{ranksArr[0], ranksArr[1]});
            }
            // 葫芦 [三条rank, 对子rank]
            return new HandValue(HandRank.FULL_HOUSE, new int[]{ranksArr[0], ranksArr[1]});
        }
        if (size == 3) {
            if (countArr[0] == 3) {
                // 三条 [三条rank, kicker1, kicker2]
                return new HandValue(HandRank.THREE_OF_A_KIND,
                        new int[]{ranksArr[0], ranksArr[1], ranksArr[2]});
            }
            // 两对 [大对rank, 小对rank, kicker]
            return new HandValue(HandRank.TWO_PAIR, new int[]{ranksArr[0], ranksArr[1], ranksArr[2]});
        }
        if (size == 4) {
            // 一对 [对子rank, kicker1..3]
            return new HandValue(HandRank.ONE_PAIR,
                    new int[]{ranksArr[0], ranksArr[1], ranksArr[2], ranksArr[3]});
        }

        // 5 张各不相同
        if (isFlush && isStraight) {
            HandRank rank = straightHigh == Card.MAX_RANK
                    ? HandRank.ROYAL_FLUSH : HandRank.STRAIGHT_FLUSH;
            return new HandValue(rank, new int[]{straightHigh});
        }
        if (isFlush) {
            return new HandValue(HandRank.FLUSH, ranksArr);
        }
        if (isStraight) {
            return new HandValue(HandRank.STRAIGHT, new int[]{straightHigh});
        }
        return new HandValue(HandRank.HIGH_CARD, ranksArr);
    }
}
