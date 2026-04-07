package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 游戏会话实体 - 用于存储石头剪刀布等游戏的状态
 */
@Data
@Entity
@Table(name = "t_game_session")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Long player1Id;

    @Column(nullable = false)
    private Long player2Id;

    // 当前回合数 (1-3)
    @Column(nullable = false)
    private Integer currentRound = 1;

    // 玩家1当前回合的选择: ROCK, PAPER, SCISSORS, null表示未选择
    @Column(length = 16)
    private String player1Choice;

    // 玩家2当前回合的选择
    @Column(length = 16)
    private String player2Choice;

    // 玩家1赢的回合数
    @Column(nullable = false)
    private Integer player1Wins = 0;

    // 玩家2赢的回合数
    @Column(nullable = false)
    private Integer player2Wins = 0;

    // 游戏状态
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.PLAYING;

    // 最终赢家ID，游戏结束时设置
    private Long winnerId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;

    public enum SessionStatus {
        PLAYING,    // 游戏进行中
        FINISHED    // 游戏结束
    }

    // 石头剪刀布选择枚举
    public enum Choice {
        ROCK,       // 石头
        PAPER,      // 布
        SCISSORS    // 剪刀
    }
}
