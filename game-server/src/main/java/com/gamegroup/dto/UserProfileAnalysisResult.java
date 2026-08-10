package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileAnalysisResult {

    private String faceFeatures;

    private String bodyFeatures;

    private String temperament;

    private String styleDirection;

    private String fitSuggestions;

    private String colorSuggestions;

    private String sceneSuggestions;

    private String summaryText;

    private String rawJson;
}
