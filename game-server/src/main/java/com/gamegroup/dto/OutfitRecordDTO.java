package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitRecordDTO {

    private Long id;

    private Long planId;

    private String targetDate;

    private String planTitle;

    private String planSummary;

    private String weatherCondition;

    private Integer temperatureMin;

    private Integer temperatureMax;

    private String feedback;

    private String createTime;
}
