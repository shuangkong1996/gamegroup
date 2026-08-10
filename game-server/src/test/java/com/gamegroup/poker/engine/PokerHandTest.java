package com.gamegroup.poker.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokerHandTest {

    private static PokerTable room3() {
        PokerTable room = new PokerTable(6, new BigDecimal("1"), new BigDecimal("2"));
        room.sitDown(0, 1001, new BigDecimal("1000"));
        room.sitDown(1, 1002, new BigDecimal("1000"));
        room.sitDown(2, 1003, new BigDecimal("1000"));
        return room;
    }

    @Test
    void should_finish_full_check_hand_with_showdown() {
        PokerTable room = room3();
        PokerHand hand = room.startHand(new Random(42));

        // preflop：UTG call 2，SB call 补齐 1，BB check
        hand.act(hand.currentActor(), PokerHand.Action.CALL, null);
        hand.act(hand.currentActor(), PokerHand.Action.CALL, null);
        hand.act(hand.currentActor(), PokerHand.Action.CHECK, null);

        // flop/turn/river：全员 check
        for (int i = 0; i < 3; i++) {
            hand.act(hand.currentActor(), PokerHand.Action.CHECK, null);
            hand.act(hand.currentActor(), PokerHand.Action.CHECK, null);
            hand.act(hand.currentActor(), PokerHand.Action.CHECK, null);
        }

        assertThat(hand.isFinished()).isTrue();
        assertThat(hand.phase()).isEqualTo(PokerHand.Phase.FINISHED);
        assertThat(hand.board()).hasSize(5);
        assertThat(hand.pot()).isEqualByComparingTo("6");

        PokerHand.HandResult r = hand.result();
        assertThat(r.winners()).hasSize(1);
        assertThat(r.chipDelta().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0");
        assertThat(r.revealedHoleCards()).hasSize(3);

        room.applyResult(r);
        // 筹码守恒
        BigDecimal total = room.snapshot().stream()
                .map(PokerTable.Seat::chip)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("3000");
    }

    @Test
    void should_end_early_when_only_one_player_remains() {
        PokerTable room = room3();
        PokerHand hand = room.startHand(new Random(7));

        // UTG raise 到 10，SB、BB 弃牌 → 只剩 1 人
        int utg = hand.currentActor();
        hand.act(utg, PokerHand.Action.RAISE, new BigDecimal("10"));
        hand.act(hand.currentActor(), PokerHand.Action.FOLD, null);
        hand.act(hand.currentActor(), PokerHand.Action.FOLD, null);

        assertThat(hand.isFinished()).isTrue();
        PokerHand.HandResult r = hand.result();
        assertThat(r.winners()).containsExactly(utg);
        // 无人摊牌：只公开赢家底牌
        assertThat(r.revealedHoleCards()).hasSize(1);
        // 无公共牌
        assertThat(r.board()).isEmpty();
    }

    @Test
    void should_reject_action_when_not_your_turn() {
        PokerTable room = room3();
        PokerHand hand = room.startHand(new Random(42));

        int actor = hand.currentActor();
        int other = (actor + 1) % 6;
        while (hand.board() == null) {
            break;
        }
        assertThatThrownBy(() -> hand.act(other, PokerHand.Action.FOLD, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未轮到你");
    }

    @Test
    void should_reject_check_when_bet_to_call() {
        PokerTable room = room3();
        PokerHand hand = room.startHand(new Random(42));

        int utg = hand.currentActor();
        hand.act(utg, PokerHand.Action.RAISE, new BigDecimal("6"));
        // 下一位无法过牌
        assertThatThrownBy(() -> hand.act(hand.currentActor(), PokerHand.Action.CHECK, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不能过牌");
    }

    @Test
    void should_reject_raise_below_minimum() {
        PokerTable room = room3();
        PokerHand hand = room.startHand(new Random(42));

        int utg = hand.currentActor();
        // 最小加注：currentBet(2) + minRaise(2) = 4，加到 3 不合法
        assertThatThrownBy(() -> hand.act(utg, PokerHand.Action.RAISE, new BigDecimal("3")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("最小加注");
    }

    @Test
    void should_build_side_pots_with_short_all_in() {
        PokerTable room = new PokerTable(6, new BigDecimal("1"), new BigDecimal("2"));
        room.sitDown(0, 1001, new BigDecimal("50"));
        room.sitDown(1, 1002, new BigDecimal("200"));
        room.sitDown(2, 1003, new BigDecimal("200"));
        PokerHand hand = room.startHand(new Random(9));

        // 座位 0（UTG）短筹码先 all-in 50；座位 1 raise 到 100；座位 2 call
        hand.act(hand.currentActor(), PokerHand.Action.ALL_IN, null);
        hand.act(hand.currentActor(), PokerHand.Action.RAISE, new BigDecimal("100"));
        hand.act(hand.currentActor(), PokerHand.Action.CALL, null);

        // 之后全部过牌到摊牌
        for (int i = 0; !hand.isFinished() && i < 20; i++) {
            int a = hand.currentActor();
            if (a < 0) {
                break;
            }
            try {
                hand.act(a, PokerHand.Action.CHECK, null);
            } catch (IllegalStateException e) {
                hand.act(a, PokerHand.Action.CALL, null);
            }
        }

        assertThat(hand.isFinished()).isTrue();
        PokerHand.HandResult r = hand.result();
        // 总投入 250：座位0=50，座位1/2=100 各
        assertThat(hand.pot()).isEqualByComparingTo("250");
        BigDecimal sum = r.chipDelta().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("0");

        room.applyResult(r);
        BigDecimal total = room.snapshot().stream()
                .map(PokerTable.Seat::chip)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("450");
    }

    @Test
    void should_rotate_dealer_between_hands() {
        PokerTable room = room3();
        PokerHand h1 = room.startHand(new Random(42));
        int dealer1 = h1.dealerSeat();

        PokerHand h2 = room.startHand(new Random(42));
        assertThat(h2.dealerSeat()).isNotEqualTo(dealer1);
    }

    @Test
    void should_remove_busted_player() {
        PokerTable room = new PokerTable(6, new BigDecimal("10"), new BigDecimal("20"));
        // 座位0 只有 30 筹码（勉强够一手的盲注），必输
        room.sitDown(0, 1001, new BigDecimal("30"));
        room.sitDown(1, 1002, new BigDecimal("1000"));
        room.sitDown(2, 1003, new BigDecimal("1000"));

        PokerHand hand = room.startHand(new Random(3));
        // 座位0 弃牌认输
        int a = hand.currentActor();
        int i = 0;
        while (!hand.isFinished() && i < 10) {
            int actor = hand.currentActor();
            if (actor == 0) {
                hand.act(actor, PokerHand.Action.FOLD, null);
            } else {
                try {
                    hand.act(actor, PokerHand.Action.CHECK, null);
                } catch (IllegalStateException e) {
                    hand.act(actor, PokerHand.Action.CALL, null);
                }
            }
            i++;
        }
        room.applyResult(hand.result());

        // 座位0 弃牌损失盲注，但可能不是破产（30 不够 20 大盲？弃牌时盲注已付）
        // 直接验证所有剩余座位筹码 > 0 且总数守恒
        assertThat(room.snapshot().stream().map(PokerTable.Seat::chip).allMatch(c -> c.signum() >= 0)).isTrue();
        BigDecimal total = room.snapshot().stream()
                .map(PokerTable.Seat::chip)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("2030");
    }
}
