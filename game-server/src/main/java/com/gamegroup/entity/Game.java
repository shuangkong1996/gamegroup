package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 512)
    private String icon;

    @Column(length = 1024)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(nullable = false)
    private Integer minPlayers = 2;

    @Column(nullable = false)
    private Integer maxPlayers = 2;

    @Column(nullable = false)
    private Boolean status = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
