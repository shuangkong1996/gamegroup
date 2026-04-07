package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnimeGenerateResponse {
    private String resultImageUrl;
    private String requestId;
}
