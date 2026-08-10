package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_poker_room")
public class PokerRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String roomNo;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Integer tableSize = 6;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal smallBlind = BigDecimal.ONE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal bigBlind = new BigDecimal("2");

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ante = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal buyInMin = new BigDecimal("100");

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal buyInMax = new BigDecimal("500");

    @Column(nullable = false)
    private Integer thinkSeconds = 20;

    @Column(nullable = false, length = 20)
    private String status = "WAITING";

    @Column(nullable = false)
    private Integer handNo = 0;

    @Column(nullable = false)
    private Integer currentPlayers = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
