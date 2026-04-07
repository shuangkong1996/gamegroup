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
public class LedgerAccountDTO {

    private Long id;

    private String name;

    private String type;

    private BigDecimal balance;

    private String icon;

    private Integer sortOrder;
}
