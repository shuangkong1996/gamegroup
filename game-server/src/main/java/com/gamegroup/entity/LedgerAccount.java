package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_ledger_account")
@NoArgsConstructor
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    @ColumnDefault("'General'")
    private String type; // Cash, Bank, Credit, Alipay, Wechat

    @Column(precision = 12, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal balance;

    @Column(length = 100)
    private String icon;

    @Column(name = "sort_order")
    @ColumnDefault("0")
    private Integer sortOrder;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
