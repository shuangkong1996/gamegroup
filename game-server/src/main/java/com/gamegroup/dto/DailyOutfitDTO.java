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
public class DailyOutfitDTO {

    private Long planId;

    private String targetDate;

    private String weatherCity;

    private String weatherCondition;

    private Integer temperatureMin;

    private Integer temperatureMax;

    private Integer humidity;

    private String planTitle;

    private String planSummary;

    private String planReason;

    private String comfortTip;

    private String status;

    private List<OutfitItemDTO> items;
}
