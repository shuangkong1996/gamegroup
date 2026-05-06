package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingRecognitionResult {

    private boolean fallback;

    private String fallbackReason;

    private String name;

    private String categoryLevel1;

    private String categoryLevel2;

    private String color;

    private String patternTags;

    private String seasonTags;

    private String styleTags;

    private String occasionTags;

    private String material;

    private Integer thicknessLevel;

    private Integer warmthLevel;

    private Integer waterproofLevel;

    private String fitType;

    private String summaryText;

    private String matchPoints;

    private String avoidPoints;

    private Double confidenceScore;

    private String rawJson;
}
