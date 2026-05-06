package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeUploadResponse {

    private Long itemId;

    private String imageUrl;

    private String recognizeStatus;
}
