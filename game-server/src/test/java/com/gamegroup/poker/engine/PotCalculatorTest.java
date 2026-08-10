package com.gamegroup.poker.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PotCalculatorTest {

    private static PotCalculator.Contributor c(int seat, String bet, boolean inHand, String... cards) {
        HandValue hand = null;
        if (inHand) {
            List<Card> list = new ArrayList<>();
            for (String code : cards) {
                list.add(Card.parse(code));
            }
            hand = HandEvaluator.evaluate(list);
        }
        return new PotCalculator.Contributor(seat, new BigDecimal(bet), inHand, hand);
    }

    @Test
    void should_give_all_to_single_winner() {
        List<PotCalculator.Contributor> players = List.of(
                c(0, "100", true, "AS", "KD", "QS", "JS", "TS"),
                c(1, "100", true, "2C", "3D", "4H", "5S", "6C"),
                c(2, "100", true, "AD", "KC", "8H", "5S", "3C"));
        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(players);
        assertThat(payouts.get(0)).isEqualByComparingTo("300");
        assertThat(payouts.get(1)).isZero();
        assertThat(payouts.get(2)).isZero();
    }

    @Test
    void should_split_pot_on_tie() {
        List<PotCalculator.Contributor> players = List.of(
                c(0, "100", true, "AS", "KS", "QS", "JS", "TS"),
                c(1, "100", true, "AD", "KD", "QD", "JD", "TD"));
        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(players);
        assertThat(payouts.get(0)).isEqualByComparingTo("100");
        assertThat(payouts.get(1)).isEqualByComparingTo("100");
    }

    @Test
    void should_give_pot_to_winner_among_two_all_in() {
        // A all-in 50，B call 50，C 弃牌
        List<PotCalculator.Contributor> players = List.of(
                c(0, "50", true, "AS", "KD", "QS", "JS", "TS"),
                c(1, "50", true, "2C", "3D", "4H", "5S", "6C"),
                c(2, "50", false, null));
        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(players);
        assertThat(payouts.get(0)).isEqualByComparingTo("150");
        assertThat(payouts.get(1)).isZero();
        assertThat(payouts.get(2)).isZero();
    }

    @Test
    void should_build_side_pots_with_three_all_in() {
        // A all-in 50，B all-in 100，C all-in 100
        // 主池 150（每人50），边池 100（B/C 各50）
        List<PotCalculator.Contributor> players = List.of(
                c(0, "50", true, "AS", "KD", "QS", "JS", "TS"),
                c(1, "100", true, "2C", "3D", "4H", "5S", "6C"),
                c(2, "100", true, "AD", "KC", "8H", "5S", "3C"));

        // 皇家同花顺 A 赢主池；B（顺子）赢边池，C（高牌）落空
        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(players);
        assertThat(payouts.get(0)).isEqualByComparingTo("150");
        assertThat(payouts.get(1)).isEqualByComparingTo("100");
        assertThat(payouts.get(2)).isZero();
    }

    @Test
    void should_award_side_pot_to_short_stack_only() {
        // A all-in 50（皇家同花顺），B bet 100 call，C bet 100 fold 后（此处 C 弃牌不参与摊牌）
        // 主池 150 = A/B/C 各50；边池 100 = B/C 各50，C 弃牌 → 边池归 B
        List<PotCalculator.Contributor> players = List.of(
                c(0, "50", true, "AS", "KD", "QS", "JS", "TS"),
                c(1, "100", true, "2C", "3D", "4H", "5S", "6C"),
                c(2, "100", false, null));
        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(players);
        assertThat(payouts.get(0)).isEqualByComparingTo("150");
        assertThat(payouts.get(1)).isEqualByComparingTo("100");
        assertThat(payouts.get(2)).isZero();
    }

    @Test
    void should_sum_payouts_equal_total_bets() {
        List<PotCalculator.Contributor> players = List.of(
                c(0, "50", true, "AS", "KD", "QS", "JS", "TS"),
                c(1, "200", true, "2C", "3D", "4H", "5S", "6C"),
                c(2, "200", true, "AD", "KC", "8H", "5S", "3C"));
        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(players);

        BigDecimal total = players.stream()
                .map(PotCalculator.Contributor::bet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sum = payouts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total);
    }

    @Test
    void should_give_remainder_to_lowest_seat_on_three_way_tie() {
        // A/B/C 各投入 100（平局），D 投入 1 弃牌
        // 主池 = 1*4 + 99*3 = 301，三人平分：A 余数 +0.01
        List<PotCalculator.Contributor> players = List.of(
                c(0, "100", true, "AS", "KS", "QS", "JS", "TS"),
                c(1, "100", true, "AD", "KD", "QD", "JD", "TD"),
                c(2, "100", true, "AH", "KH", "QH", "JH", "TH"),
                c(3, "1", false, null));
        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(players);
        BigDecimal s0 = payouts.get(0);
        BigDecimal s1 = payouts.get(1);
        BigDecimal s2 = payouts.get(2);
        assertThat(s0.add(s1).add(s2)).isEqualByComparingTo("301");
        assertThat(s0).isEqualByComparingTo("100.34");
        assertThat(s1).isEqualByComparingTo("100.33");
        assertThat(s2).isEqualByComparingTo("100.33");
    }
}
