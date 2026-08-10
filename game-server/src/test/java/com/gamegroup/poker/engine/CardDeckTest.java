package com.gamegroup.poker.engine;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardDeckTest {

    @Test
    void should_parse_card_code_round_trip() {
        assertThat(Card.parse("AS").toString()).isEqualTo("AS");
        assertThat(Card.parse("TH").toString()).isEqualTo("TH");
        assertThat(Card.parse("2C").toString()).isEqualTo("2C");
        assertThat(Card.parse("9D").toString()).isEqualTo("9D");
        assertThat(Card.parse("ks").toString()).isEqualTo("KS");
        assertThat(Card.parse("qH").toString()).isEqualTo("QH");
    }

    @Test
    void should_parse_correct_rank_and_suit() {
        Card ace = Card.parse("AS");
        assertThat(ace.rank()).isEqualTo(14);
        assertThat(ace.suit()).isEqualTo('S');

        Card ten = Card.parse("TD");
        assertThat(ten.rank()).isEqualTo(10);
        assertThat(ten.suit()).isEqualTo('D');
    }

    @Test
    void should_reject_invalid_codes() {
        assertThatThrownBy(() -> Card.parse("1S")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Card.parse("XX")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Card.parse("A")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Card(1, 'S')).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Card(15, 'S')).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Card(5, 'X')).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_have_52_unique_cards() {
        Deck deck = new Deck();
        assertThat(deck.remaining()).isEqualTo(52);

        java.util.HashSet<String> seen = new java.util.HashSet<>();
        while (!deck.isEmpty()) {
            Card card = deck.deal();
            assertThat(seen.add(card.toString())).as("重复的牌: %s", card).isTrue();
        }
        assertThat(seen).hasSize(52);
        assertThat(deck.remaining()).isZero();
    }

    @Test
    void should_throw_when_deck_empty() {
        Deck deck = new Deck();
        for (int i = 0; i < 52; i++) {
            deck.deal();
        }
        assertThatThrownBy(deck::deal).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_reproduce_same_order_with_same_seed() {
        Deck d1 = new Deck();
        d1.shuffle(new Random(42L));
        Deck d2 = new Deck();
        d2.shuffle(new Random(42L));

        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        for (int i = 0; i < 52; i++) {
            s1.append(d1.deal());
            s2.append(d2.deal());
        }
        assertThat(s1.toString()).isEqualTo(s2.toString());
    }
}
