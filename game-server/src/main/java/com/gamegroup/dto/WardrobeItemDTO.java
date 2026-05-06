package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeItemDTO {

    private Long id;

    private String imageUrl;

    private String name;

    private String categoryLevel1;

    private String categoryLevel2;

    private String color;

    private String seasonTags;

    private String styleTags;

    private String recognizeStatus;

    private String status;

    private String createTime;
}
