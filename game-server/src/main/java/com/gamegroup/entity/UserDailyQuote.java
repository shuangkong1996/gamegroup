package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日语录关联实体
 */
@Data
@Entity
@Table(name = "t_user_daily_quote", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "quoteDate"})
})
public class UserDailyQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long quoteId;

    @Column(nullable = false)
    private LocalDate quoteDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
