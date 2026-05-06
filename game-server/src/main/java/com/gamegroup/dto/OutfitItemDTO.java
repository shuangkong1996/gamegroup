package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitItemDTO {

    private Long itemId;

    private String itemRole;

    private String name;

    private String imageUrl;

    private String categoryLevel1;

    private String color;
}
