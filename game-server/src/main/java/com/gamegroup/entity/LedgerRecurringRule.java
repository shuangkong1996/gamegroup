package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "t_ledger_recurring_rule",
        indexes = {
                @Index(name = "idx_rule_status", columnList = "status")
        }
)
@NoArgsConstructor
public class LedgerRecurringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * 1-expense (支出), 2-income (收入)
     */
    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(length = 255)
    private String remark;

    /**
     * yyyy-MM
     */
    @Column(name = "start_month", nullable = false, length = 7)
    private String startMonth;

    /**
     * yyyy-MM, NULL means forever
     */
    @Column(name = "end_month", length = 7)
    private String endMonth;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE/PAUSED

    @Column(name = "last_generated_month", length = 7)
    private String lastGeneratedMonth;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (updateTime == null) {
            updateTime = createTime;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}

