package com.gamegroup.poker.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 标准 52 张牌牌堆（纯 Java）。
 * 洗牌支持注入 Random seed，便于测试复现同一手牌。
 */
public class Deck {

    private final List<Card> cards;
    private int next;

    public Deck() {
        cards = new ArrayList<>(52);
        for (char suit : new char[]{'S', 'H', 'D', 'C'}) {
            for (int rank = Card.MIN_RANK; rank <= Card.MAX_RANK; rank++) {
                cards.add(new Card(rank, suit));
            }
        }
        next = 0;
    }

    /**
     * 使用指定随机源洗牌（seed 固定可复现）。
     */
    public void shuffle(Random random) {
        Collections.shuffle(cards, random);
        next = 0;
    }

    public void shuffle() {
        shuffle(new Random());
    }

    /**
     * 发一张牌，牌堆耗尽时抛出异常。
     */
    public Card deal() {
        if (next >= cards.size()) {
            throw new IllegalStateException("牌堆已耗尽");
        }
        return cards.get(next++);
    }

    public boolean isEmpty() {
        return next >= cards.size();
    }

    public int remaining() {
        return cards.size() - next;
    }
}
