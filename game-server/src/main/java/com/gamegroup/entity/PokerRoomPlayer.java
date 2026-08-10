package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_poker_room_player",
        uniqueConstraints = @UniqueConstraint(columnNames = {"roomId", "seat"}))
public class PokerRoomPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer seat;

    @Column(length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatarUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal chip;

    @Column(nullable = false, length = 20)
    private String status = "SEATED";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinTime;

    private LocalDateTime leaveTime;
}
