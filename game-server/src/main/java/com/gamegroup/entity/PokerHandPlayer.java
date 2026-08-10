package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_poker_hand_player")
public class PokerHandPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long handId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer seat;

    @Column(length = 20)
    private String holeCards;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startingChip;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal betTotal;

    @Column(length = 40)
    private String handRank;

    @Column(length = 20)
    private String result;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal winAmount;
}
