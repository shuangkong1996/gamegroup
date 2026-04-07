package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerCategoryStatsResponse {

    private List<LedgerCategoryStatsItemDTO> expense;

    private List<LedgerCategoryStatsItemDTO> income;
}
