package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_match_room")
public class MatchRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, unique = true, length = 8)
    private String verifyCode;

    @Column(nullable = false)
    private Long creatorId;

    @Column(length = 512)
    private String playerIds;

    @Column(nullable = false)
    private Integer currentPlayers = 1;

    @Column(nullable = false)
    private Integer maxPlayers = 2;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomStatus status = RoomStatus.WAITING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime expireTime;

    public enum RoomStatus {
        WAITING,
        MATCHED,
        PLAYING,
        FINISHED,
        EXPIRED
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
