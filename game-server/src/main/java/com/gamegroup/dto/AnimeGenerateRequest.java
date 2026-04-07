package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnimeGenerateRequest {
    
    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;
}
