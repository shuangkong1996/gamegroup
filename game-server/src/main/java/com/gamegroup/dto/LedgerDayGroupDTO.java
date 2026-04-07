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
public class LedgerDayGroupDTO {

    private String date;

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private List<LedgerRecordDTO> records;
}
