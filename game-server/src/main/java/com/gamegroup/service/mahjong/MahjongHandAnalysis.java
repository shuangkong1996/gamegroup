package com.gamegroup.service.mahjong;

import java.util.Collections;
import java.util.List;

/**
 * 手牌规则分析结果。
 */
public class MahjongHandAnalysis {

    private final boolean valid;
    private final int tileCount;
    private final List<String> recognizedTiles;
    private final boolean alreadyWinning;
    private final List<String> winningTiles;
    private final String handForm;
    private final String suggestion;
    private final String reason;

    private MahjongHandAnalysis(
            boolean valid,
            int tileCount,
            List<String> recognizedTiles,
            boolean alreadyWinning,
            List<String> winningTiles,
            String handForm,
            String suggestion,
            String reason) {
        this.valid = valid;
        this.tileCount = tileCount;
        this.recognizedTiles = recognizedTiles == null ? List.of() : List.copyOf(recognizedTiles);
        this.alreadyWinning = alreadyWinning;
        this.winningTiles = winningTiles == null ? List.of() : List.copyOf(winningTiles);
        this.handForm = handForm;
        this.suggestion = suggestion;
        this.reason = reason;
    }

    public static MahjongHandAnalysis invalid(List<String> recognizedTiles, String reason, String suggestion) {
        int count = recognizedTiles == null ? 0 : recognizedTiles.size();
        return new MahjongHandAnalysis(
                false,
                count,
                recognizedTiles,
                false,
                Collections.emptyList(),
                null,
                suggestion,
                reason);
    }

    public static MahjongHandAnalysis winning(List<String> recognizedTiles, String handForm, String suggestion) {
        return new MahjongHandAnalysis(
                true,
                recognizedTiles.size(),
                recognizedTiles,
                true,
                Collections.emptyList(),
                handForm,
                suggestion,
                null);
    }

    public static MahjongHandAnalysis waiting(
            List<String> recognizedTiles,
            List<String> winningTiles,
            String handForm,
            String suggestion) {
        return new MahjongHandAnalysis(
                true,
                recognizedTiles.size(),
                recognizedTiles,
                false,
                winningTiles,
                handForm,
                suggestion,
                null);
    }

    public static MahjongHandAnalysis notReady(List<String> recognizedTiles, String suggestion) {
        return new MahjongHandAnalysis(
                true,
                recognizedTiles.size(),
                recognizedTiles,
                false,
                Collections.emptyList(),
                null,
                suggestion,
                null);
    }

    public boolean isValid() {
        return valid;
    }

    public int getTileCount() {
        return tileCount;
    }

    public List<String> getRecognizedTiles() {
        return recognizedTiles;
    }

    public boolean isAlreadyWinning() {
        return alreadyWinning;
    }

    public List<String> getWinningTiles() {
        return winningTiles;
    }

    public String getHandForm() {
        return handForm;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public String getReason() {
        return reason;
    }
}
