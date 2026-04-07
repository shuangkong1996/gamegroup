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
public class LedgerCategoryGroupDTO {

    private List<LedgerCategoryDTO> expense;

    private List<LedgerCategoryDTO> income;
}
