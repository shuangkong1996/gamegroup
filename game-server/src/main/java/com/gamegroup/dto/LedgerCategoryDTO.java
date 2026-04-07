package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerCategoryDTO {

    private Long id;

    private String name;

    private Integer type;

    private String icon;

    private Integer sortOrder;
}
