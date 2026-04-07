package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_ledger_record", indexes = {
        @Index(name = "idx_user_time", columnList = "user_id, record_time"),
        @Index(name = "idx_account", columnList = "account_id")
})
@NoArgsConstructor
public class LedgerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * 1: Expense (支出), 2: Income (收入)
     */
    @Column(nullable = false)
    private Integer type;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @Column(length = 255)
    private String remark;

    // Redundant fields for quick query (snapshot)
    @Column(name = "category_name", length = 50)
    private String categoryName;

    @Column(name = "category_icon", length = 100)
    private String categoryIcon;

    @Column(name = "account_name", length = 50)
    private String accountName;

    // Idempotency / trace for recurring rule generated records
    @Column(name = "recurring_rule_id")
    private Long recurringRuleId;

    @Column(name = "recurring_month", length = 7)
    private String recurringMonth; // yyyy-MM

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}
