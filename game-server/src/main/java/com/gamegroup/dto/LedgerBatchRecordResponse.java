package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerBatchRecordResponse {

    private Integer createdCount;

    private BigDecimal totalAmount;

    private List<String> dates;
}
