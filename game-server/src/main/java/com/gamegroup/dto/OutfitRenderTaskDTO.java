package com.gamegroup.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitRenderTaskDTO {

    private Long taskId;

    private String mode;

    private String status;

    private String targetDate;

    private String weatherCity;

    private String weatherCondition;

    private Integer temperatureMin;

    private Integer temperatureMax;

    private Integer humidity;

    private String sceneRequirement;

    private Boolean useWardrobeItems;

    private String planTitle;

    private String planSummary;

    private String planReason;

    private String comfortTip;

    private String styleKeywords;

    private String sceneKeywords;

    private String resultImageUrl;

    private String errorMessage;

    private List<OutfitRenderTaskItemDTO> items;
}
