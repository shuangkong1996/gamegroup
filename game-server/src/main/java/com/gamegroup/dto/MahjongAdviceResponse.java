package com.gamegroup.dto;

import java.util.List;

public class MahjongAdviceResponse {

    private String imageUrl;
    private String handSummary;
    private Boolean alreadyWinning;
    private List<String> winningTiles;
    private List<String> recognizedTiles;
    private Integer tileCount;
    private Boolean handValid;
    private String handForm;
    private String analysisMode;
    private String suggestion;
    private String confidenceLabel;
    private String rawResult;

    public MahjongAdviceResponse() {
    }

    public MahjongAdviceResponse(
            String imageUrl,
            String handSummary,
            Boolean alreadyWinning,
            List<String> winningTiles,
            List<String> recognizedTiles,
            Integer tileCount,
            Boolean handValid,
            String handForm,
            String analysisMode,
            String suggestion,
            String confidenceLabel,
            String rawResult) {
        this.imageUrl = imageUrl;
        this.handSummary = handSummary;
        this.alreadyWinning = alreadyWinning;
        this.winningTiles = winningTiles;
        this.recognizedTiles = recognizedTiles;
        this.tileCount = tileCount;
        this.handValid = handValid;
        this.handForm = handForm;
        this.analysisMode = analysisMode;
        this.suggestion = suggestion;
        this.confidenceLabel = confidenceLabel;
        this.rawResult = rawResult;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getHandSummary() {
        return handSummary;
    }

    public void setHandSummary(String handSummary) {
        this.handSummary = handSummary;
    }

    public Boolean getAlreadyWinning() {
        return alreadyWinning;
    }

    public void setAlreadyWinning(Boolean alreadyWinning) {
        this.alreadyWinning = alreadyWinning;
    }

    public List<String> getWinningTiles() {
        return winningTiles;
    }

    public void setWinningTiles(List<String> winningTiles) {
        this.winningTiles = winningTiles;
    }

    public List<String> getRecognizedTiles() {
        return recognizedTiles;
    }

    public void setRecognizedTiles(List<String> recognizedTiles) {
        this.recognizedTiles = recognizedTiles;
    }

    public Integer getTileCount() {
        return tileCount;
    }

    public void setTileCount(Integer tileCount) {
        this.tileCount = tileCount;
    }

    public Boolean getHandValid() {
        return handValid;
    }

    public void setHandValid(Boolean handValid) {
        this.handValid = handValid;
    }

    public String getHandForm() {
        return handForm;
    }

    public void setHandForm(String handForm) {
        this.handForm = handForm;
    }

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getConfidenceLabel() {
        return confidenceLabel;
    }

    public void setConfidenceLabel(String confidenceLabel) {
        this.confidenceLabel = confidenceLabel;
    }

    public String getRawResult() {
        return rawResult;
    }

    public void setRawResult(String rawResult) {
        this.rawResult = rawResult;
    }
}
