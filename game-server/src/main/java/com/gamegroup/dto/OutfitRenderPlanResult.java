package com.gamegroup.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitRenderPlanResult {

    private String planTitle;

    private String planSummary;

    private String planReason;

    private String comfortTip;

    private String styleKeywords;

    private String sceneKeywords;

    private String renderPrompt;

    private String profileAnalysisSummary;

    private String planJson;

    @Builder.Default
    private List<OutfitRenderTaskItemDTO> items = new ArrayList<>();
}
