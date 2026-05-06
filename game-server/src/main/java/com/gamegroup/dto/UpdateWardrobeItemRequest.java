package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateWardrobeItemRequest {

    @NotBlank(message = "衣物名称不能为空")
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
}
