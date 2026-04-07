package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_ledger_category")
@NoArgsConstructor
public class LedgerCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 0 for system default

    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 1: Expense (支出), 2: Income (收入)
     */
    @Column(nullable = false)
    private Integer type;

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
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
