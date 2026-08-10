package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_poker_hand",
        uniqueConstraints = @UniqueConstraint(columnNames = {"roomId", "handNo"}))
public class PokerHandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Integer handNo;

    @Column(nullable = false)
    private Integer dealerSeat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal smallBlind;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal bigBlind;

    @Column(length = 40)
    private String boardCards;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pot;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;
}
