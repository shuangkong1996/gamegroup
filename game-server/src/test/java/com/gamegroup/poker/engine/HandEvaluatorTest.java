package com.gamegroup.poker.engine;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandEvaluatorTest {

    private static HandValue eval(String... codes) {
        List<Card> cards = Arrays.stream(codes).map(Card::parse).collect(Collectors.toList());
        return HandEvaluator.evaluate(cards);
    }

    @Test
    void should_identify_royal_flush() {
        HandValue v = eval("AS", "KS", "QS", "JS", "TS");
        assertThat(v.rank()).isEqualTo(HandRank.ROYAL_FLUSH);
    }

    @Test
    void should_identify_straight_flush() {
        HandValue v = eval("9S", "8S", "7S", "6S", "5S");
        assertThat(v.rank()).isEqualTo(HandRank.STRAIGHT_FLUSH);
        assertThat(v.tieBreakers()).containsExactly(9);
    }

    @Test
    void should_identify_four_of_a_kind() {
        HandValue v = eval("9D", "9C", "9H", "9S", "AS");
        assertThat(v.rank()).isEqualTo(HandRank.FOUR_OF_A_KIND);
        assertThat(v.tieBreakers()).containsExactly(9, 14);
    }

    @Test
    void should_identify_full_house() {
        HandValue v = eval("6D", "6C", "6H", "3S", "3C");
        assertThat(v.rank()).isEqualTo(HandRank.FULL_HOUSE);
        assertThat(v.tieBreakers()).containsExactly(6, 3);
    }

    @Test
    void should_identify_flush() {
        HandValue v = eval("AH", "JH", "9H", "6H", "2H");
        assertThat(v.rank()).isEqualTo(HandRank.FLUSH);
        assertThat(v.tieBreakers()).containsExactly(14, 11, 9, 6, 2);
    }

    @Test
    void should_identify_straight() {
        HandValue v = eval("9C", "8D", "7H", "6S", "5C");
        assertThat(v.rank()).isEqualTo(HandRank.STRAIGHT);
        assertThat(v.tieBreakers()).containsExactly(9);
    }

    @Test
    void should_identify_three_of_a_kind() {
        HandValue v = eval("7D", "7C", "7H", "KS", "2C");
        assertThat(v.rank()).isEqualTo(HandRank.THREE_OF_A_KIND);
        assertThat(v.tieBreakers()).containsExactly(7, 13, 2);
    }

    @Test
    void should_identify_two_pair() {
        HandValue v = eval("QD", "QC", "5H", "5S", "9C");
        assertThat(v.rank()).isEqualTo(HandRank.TWO_PAIR);
        assertThat(v.tieBreakers()).containsExactly(12, 5, 9);
    }

    @Test
    void should_identify_one_pair() {
        HandValue v = eval("JH", "JD", "8S", "4C", "2H");
        assertThat(v.rank()).isEqualTo(HandRank.ONE_PAIR);
        assertThat(v.tieBreakers()).containsExactly(11, 8, 4, 2);
    }

    @Test
    void should_identify_high_card() {
        HandValue v = eval("AD", "KC", "8H", "5S", "3C");
        assertThat(v.rank()).isEqualTo(HandRank.HIGH_CARD);
        assertThat(v.tieBreakers()).containsExactly(14, 13, 8, 5, 3);
    }

    @Test
    void should_recognize_wheel_straight_as_five_high() {
        HandValue v = eval("AH", "2C", "3D", "4S", "5H");
        assertThat(v.rank()).isEqualTo(HandRank.STRAIGHT);
        assertThat(v.tieBreakers()).containsExactly(5);
    }

    @Test
    void should_pick_best_five_out_of_seven() {
        // 7 张中应选出皇家同花顺
        HandValue v = eval("AS", "KS", "QS", "JS", "TS", "5D", "2C");
        assertThat(v.rank()).isEqualTo(HandRank.ROYAL_FLUSH);

        // 7 张中应选出顺子（忽略杂牌对）
        HandValue v2 = eval("9C", "8D", "7H", "6S", "5C", "AD", "2D");
        assertThat(v2.rank()).isEqualTo(HandRank.STRAIGHT);

        // 7 张中同花与顺子并存，取同花顺（此处选 straight flush）
        HandValue v3 = eval("7S", "8S", "9S", "6S", "5S", "4D", "KC");
        assertThat(v3.rank()).isEqualTo(HandRank.STRAIGHT_FLUSH);
    }

    @Test
    void should_compare_pair_kickers() {
        HandValue higher = eval("KS", "KC", "8H", "4S", "2D");
        HandValue lower = eval("KH", "KD", "7C", "5S", "3D");
        assertThat(higher.compareTo(lower)).isPositive();
        assertThat(lower.compareTo(higher)).isNegative();
    }

    @Test
    void should_compare_two_pair_by_high_pair() {
        HandValue higher = eval("AS", "AC", "QS", "QH", "2D");
        HandValue lower = eval("KD", "KC", "QS", "QH", "2C");
        assertThat(higher.compareTo(lower)).isPositive();
    }

    @Test
    void should_compare_full_house_by_trips() {
        HandValue higher = eval("7S", "7H", "7D", "2C", "2S");
        HandValue lower = eval("6D", "6C", "6H", "AS", "AH");
        assertThat(higher.compareTo(lower)).isPositive();
    }

    @Test
    void should_compare_straight_flush_vs_four_of_a_kind() {
        HandValue sf = eval("9S", "8S", "7S", "6S", "5S");
        HandValue quads = eval("AS", "AD", "AC", "AH", "2D");
        assertThat(sf.compareTo(quads)).isPositive();
    }

    @Test
    void should_compare_flush_by_kickers() {
        HandValue lower = eval("AH", "9H", "7H", "5H", "2H");
        HandValue higher = eval("AC", "9C", "7C", "5C", "3C");
        assertThat(higher.compareTo(lower)).isPositive();
    }

    @Test
    void should_tie_on_identical_hands() {
        HandValue a = eval("AS", "KS", "QS", "JS", "TS");
        HandValue b = eval("AD", "KD", "QD", "JD", "TD");
        assertThat(a.compareTo(b)).isZero();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void should_reject_invalid_card_count() {
        assertThatThrownBy(() -> eval("AS", "KS", "QS", "JS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> eval("AS", "KS", "QS", "JS", "TS", "9D", "8C", "7H"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_return_best_five_sorted_desc() {
        // 7 张中最佳 5 张 = 皇家同花顺
        HandEvaluator.EvaluatedHand eh = HandEvaluator.evaluateWithBestFive(
                Arrays.stream(new String[]{"AS", "KS", "QS", "JS", "TS", "5D", "2C"})
                        .map(Card::parse).toList());
        assertThat(eh.value().rank()).isEqualTo(HandRank.ROYAL_FLUSH);
        assertThat(eh.bestFive()).hasSize(5);
        assertThat(eh.bestFive().stream().map(Card::toString).toList())
                .containsExactly("AS", "KS", "QS", "JS", "TS");

        // 7 张中最佳 5 张 = 对子 + 3 踢脚，按点数降序
        HandEvaluator.EvaluatedHand eh2 = HandEvaluator.evaluateWithBestFive(
                Arrays.stream(new String[]{"9C", "9D", "2H", "3S", "5C", "KD", "7H"})
                        .map(Card::parse).toList());
        assertThat(eh2.value().rank()).isEqualTo(HandRank.ONE_PAIR);
        List<Integer> ranks = eh2.bestFive().stream().map(Card::rank).toList();
        for (int i = 0; i < ranks.size() - 1; i++) {
            assertThat(ranks.get(i)).isGreaterThanOrEqualTo(ranks.get(i + 1));
        }
    }
}
