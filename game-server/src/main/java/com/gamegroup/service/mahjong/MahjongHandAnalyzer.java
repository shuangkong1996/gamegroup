package com.gamegroup.service.mahjong;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准麻将听牌/胡牌规则引擎。
 * 支持：标准型（4 面子 + 1 将）、七对、十三幺。
 * <p>
 * 张数规则：
 * - 无副露：13 听 / 14 胡
 * - 已碰/杠可不录副露，只录剩余手牌：
 *   1 组副露 → 10 听 / 11 胡
 *   2 组副露 → 7 听 / 8 胡
 *   3 组副露 → 4 听 / 5 胡
 *   4 组副露 → 1 听 / 2 胡
 * 不含癞子与地方特殊规则。
 */
@Component
public class MahjongHandAnalyzer {

    public MahjongHandAnalysis analyze(List<String> rawTiles) {
        if (rawTiles == null || rawTiles.isEmpty()) {
            return MahjongHandAnalysis.invalid(
                    List.of(),
                    "EMPTY_HAND",
                    "没有识别到手牌，请重新拍摄更清晰的手牌照片");
        }

        final int[] counts;
        try {
            counts = MahjongTileCodec.toCounts(rawTiles);
        } catch (IllegalArgumentException ex) {
            return MahjongHandAnalysis.invalid(
                    List.of(),
                    "UNKNOWN_TILE",
                    "识别到无法解析的牌名，请重新拍摄或换个角度再试");
        }

        if (MahjongTileCodec.hasOverflow(counts)) {
            List<String> sorted = MahjongTileCodec.toSortedNames(counts);
            return MahjongHandAnalysis.invalid(
                    sorted,
                    "TILE_OVERFLOW",
                    "同一张牌超过 4 张，识别结果不可靠，请重新拍摄");
        }

        List<String> sortedTiles = MahjongTileCodec.toSortedNames(counts);
        int total = MahjongTileCodec.totalTiles(counts);

        if (isWinningCount(total)) {
            String form = detectWinningForm(counts);
            if (form != null) {
                int assumedMelds = assumedExposedMeldsForWinning(total);
                String meldHint = assumedMelds > 0
                        ? "（按剩余 " + total + " 张手牌估算，约有 " + assumedMelds + " 组碰/杠副露）"
                        : "";
                return MahjongHandAnalysis.winning(
                        sortedTiles,
                        form,
                        "胡了哦！当前手牌已胡（" + form + "）" + meldHint);
            }
            return MahjongHandAnalysis.notReady(
                    sortedTiles,
                    buildNotWinningSuggestion(total));
        }

        if (isWaitingCount(total)) {
            List<String> waits = findWaitingTiles(counts);
            int assumedMelds = assumedExposedMeldsForWaiting(total);
            String meldHint = assumedMelds > 0
                    ? "（剩余 " + total + " 张，按约 " + assumedMelds + " 组碰/杠估算）"
                    : "";
            if (!waits.isEmpty()) {
                return MahjongHandAnalysis.waiting(
                        sortedTiles,
                        waits,
                        "听牌",
                        "当前听牌" + meldHint + "，可胡：" + String.join("、", waits));
            }
            return MahjongHandAnalysis.notReady(
                    sortedTiles,
                    "当前 " + total + " 张还未形成听牌" + meldHint
                            + "。可继续改牌，或确认是否漏录/多录了手牌");
        }

        return MahjongHandAnalysis.invalid(
                sortedTiles,
                "UNSUPPORTED_COUNT",
                "当前 " + total + " 张不在可计算范围内。"
                        + "无副露：13 听 / 14 胡；"
                        + "碰/杠 1 次：10 听 / 11 胡；"
                        + "2 次：7 听 / 8 胡；"
                        + "3 次：4 听 / 5 胡；"
                        + "4 次：1 听 / 2 胡。"
                        + "已碰杠可不录副露，只录剩余手牌即可");
    }

    private String buildNotWinningSuggestion(int total) {
        if (total == 14) {
            return "当前 14 张尚未胡牌。若刚摸牌，可考虑先打出一张再听牌";
        }
        int assumedMelds = assumedExposedMeldsForWinning(total);
        return "当前 " + total + " 张尚未胡牌"
                + (assumedMelds > 0 ? "（按约 " + assumedMelds + " 组碰/杠估算）" : "")
                + "。可继续改牌后再算";
    }

    private List<String> findWaitingTiles(int[] sourceCounts) {
        List<String> waits = new ArrayList<>();
        int[] counts = sourceCounts.clone();
        for (int i = 0; i < MahjongTileCodec.TILE_TYPE_COUNT; i++) {
            if (counts[i] >= MahjongTileCodec.MAX_SAME_TILE) {
                continue;
            }
            counts[i]++;
            if (detectWinningForm(counts) != null) {
                waits.add(MahjongTileCodec.nameOf(i));
            }
            counts[i]--;
        }
        return waits;
    }

    /**
     * 胡牌张数：2/5/8/11/14（= 2 + 3k），对应封闭面子数 k，副露约 4-k 组。
     */
    private boolean isWinningCount(int total) {
        return total >= 2 && total <= 14 && (total - 2) % 3 == 0;
    }

    /**
     * 听牌张数：1/4/7/10/13（= 1 + 3k）。
     */
    private boolean isWaitingCount(int total) {
        return total >= 1 && total <= 13 && (total - 1) % 3 == 0;
    }

    private int assumedExposedMeldsForWinning(int total) {
        int closedMelds = (total - 2) / 3;
        return Math.max(0, 4 - closedMelds);
    }

    private int assumedExposedMeldsForWaiting(int total) {
        // 听牌再进 1 张后应变成胡牌张数
        return assumedExposedMeldsForWinning(total + 1);
    }

    private String detectWinningForm(int[] counts) {
        int total = MahjongTileCodec.totalTiles(counts);
        if (!isWinningCount(total)) {
            return null;
        }

        // 七对、十三幺仅完整门清 14 张
        if (total == 14) {
            if (isSevenPairs(counts)) {
                return "七对";
            }
            if (isThirteenOrphans(counts)) {
                return "十三幺";
            }
        }

        int meldsNeeded = (total - 2) / 3;
        if (isStandardWin(counts, meldsNeeded)) {
            return "标准型";
        }
        return null;
    }

    private boolean isSevenPairs(int[] counts) {
        int pairs = 0;
        for (int count : counts) {
            if (count == 2) {
                pairs++;
            } else if (count != 0) {
                return false;
            }
        }
        return pairs == 7;
    }

    private boolean isThirteenOrphans(int[] counts) {
        int[] terminals = {0, 8, 9, 17, 18, 26, 27, 28, 29, 30, 31, 32, 33};
        int pairCount = 0;
        for (int index : terminals) {
            int count = counts[index];
            if (count == 0 || count > 2) {
                return false;
            }
            if (count == 2) {
                pairCount++;
            }
        }
        if (pairCount != 1) {
            return false;
        }
        for (int i = 0; i < MahjongTileCodec.TILE_TYPE_COUNT; i++) {
            boolean terminal = false;
            for (int t : terminals) {
                if (t == i) {
                    terminal = true;
                    break;
                }
            }
            if (!terminal && counts[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isStandardWin(int[] counts, int meldsNeeded) {
        if (meldsNeeded < 0 || meldsNeeded > 4) {
            return false;
        }
        if (MahjongTileCodec.totalTiles(counts) != meldsNeeded * 3 + 2) {
            return false;
        }

        int[] work = counts.clone();
        for (int i = 0; i < work.length; i++) {
            if (work[i] < 2) {
                continue;
            }
            work[i] -= 2;
            if (canFormMelds(work, meldsNeeded)) {
                return true;
            }
            work[i] += 2;
        }
        return false;
    }

    private boolean canFormMelds(int[] counts, int meldsLeft) {
        if (meldsLeft == 0) {
            return MahjongTileCodec.totalTiles(counts) == 0;
        }

        int i = firstNonZero(counts);
        if (i < 0) {
            return false;
        }

        // 刻子
        if (counts[i] >= 3) {
            counts[i] -= 3;
            if (canFormMelds(counts, meldsLeft - 1)) {
                counts[i] += 3;
                return true;
            }
            counts[i] += 3;
        }

        // 顺子（仅数牌）
        if (i < 27) {
            int rank = i % 9;
            if (rank <= 6 && counts[i] > 0 && counts[i + 1] > 0 && counts[i + 2] > 0) {
                counts[i]--;
                counts[i + 1]--;
                counts[i + 2]--;
                if (canFormMelds(counts, meldsLeft - 1)) {
                    counts[i]++;
                    counts[i + 1]++;
                    counts[i + 2]++;
                    return true;
                }
                counts[i]++;
                counts[i + 1]++;
                counts[i + 2]++;
            }
        }

        return false;
    }

    private int firstNonZero(int[] counts) {
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                return i;
            }
        }
        return -1;
    }
}
