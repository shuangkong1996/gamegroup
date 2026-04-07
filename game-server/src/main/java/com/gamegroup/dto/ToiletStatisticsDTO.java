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
public class ToiletStatisticsDTO {

    private Integer totalDuration;

    private BigDecimal totalMoney;

    private Long recordCount;
}
