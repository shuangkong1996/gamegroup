package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerRecordDTO {

    private Long id;

    private Long accountId;

    private Long categoryId;

    private BigDecimal amount;

    private Integer type;

    private LocalDateTime recordTime;

    private String remark;

    private String categoryName;

    private String categoryIcon;

    private String accountName;
}
