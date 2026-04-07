package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerCategoryStatsItemDTO {

    private Long categoryId;

    private String categoryName;

    private String categoryIcon;

    private BigDecimal totalAmount;

    private BigDecimal percentage;
}
