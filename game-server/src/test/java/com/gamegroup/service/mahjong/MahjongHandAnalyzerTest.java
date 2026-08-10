package com.gamegroup.service.mahjong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MahjongHandAnalyzerTest {

    private MahjongHandAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new MahjongHandAnalyzer();
    }

    @Test
    void should_detect_standard_winning_hand() {
        // 123m 456m 789m 123p 东东
        List<String> tiles = List.of(
                "一万", "二万", "三万",
                "四万", "五万", "六万",
                "七万", "八万", "九万",
                "一筒", "二筒", "三筒",
                "东风", "东风");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertTrue(analysis.isValid());
        assertTrue(analysis.isAlreadyWinning());
        assertEquals("标准型", analysis.getHandForm());
        assertTrue(analysis.getWinningTiles().isEmpty());
    }

    @Test
    void should_detect_seven_pairs() {
        List<String> tiles = List.of(
                "一万", "一万",
                "二筒", "二筒",
                "三条", "三条",
                "四万", "四万",
                "五筒", "五筒",
                "六条", "六条",
                "红中", "红中");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertTrue(analysis.isAlreadyWinning());
        assertEquals("七对", analysis.getHandForm());
    }

    @Test
    void should_detect_waiting_tiles_for_13_hand() {
        // 123m 456m 789m 123p 东  => 听东
        List<String> tiles = List.of(
                "一万", "二万", "三万",
                "四万", "五万", "六万",
                "七万", "八万", "九万",
                "一筒", "二筒", "三筒",
                "东风");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertTrue(analysis.isValid());
        assertFalse(analysis.isAlreadyWinning());
        assertEquals(List.of("东风"), analysis.getWinningTiles());
    }

    @Test
    void should_detect_two_sided_wait() {
        // 23m 456m 789m 123p 55s => 听1万/4万
        List<String> tiles = List.of(
                "二万", "三万",
                "四万", "五万", "六万",
                "七万", "八万", "九万",
                "一筒", "二筒", "三筒",
                "五条", "五条");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertFalse(analysis.isAlreadyWinning());
        // 23m + 456m + 789m 在数学上可听 1/4/7 万
        assertEquals(List.of("一万", "四万", "七万"), analysis.getWinningTiles());
    }

    @Test
    void should_reject_invalid_tile_count() {
        List<String> tiles = List.of("一万", "二万", "三万");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertFalse(analysis.isValid());
        assertFalse(analysis.isAlreadyWinning());
        assertTrue(analysis.getWinningTiles().isEmpty());
    }

    @Test
    void should_normalize_aliases_before_analyze() {
        List<String> tiles = new ArrayList<>();
        tiles.add("1万");
        tiles.add("2万");
        tiles.add("3万");
        tiles.add("4万");
        tiles.add("5万");
        tiles.add("6万");
        tiles.add("7万");
        tiles.add("8万");
        tiles.add("9万");
        tiles.add("1筒");
        tiles.add("2饼");
        tiles.add("3饼");
        tiles.add("东");
        tiles.add("东风");

        // 先归一再分析
        List<String> normalized = MahjongTileCodec.normalizeAndSort(tiles);
        MahjongHandAnalysis analysis = analyzer.analyze(normalized);

        assertTrue(analysis.isAlreadyWinning());
        assertEquals("标准型", analysis.getHandForm());
    }

    @Test
    void should_detect_thirteen_orphans_wait() {
        // 十三幺差白板
        List<String> tiles = List.of(
                "一万", "九万",
                "一筒", "九筒",
                "一条", "九条",
                "东风", "南风", "西风", "北风",
                "红中", "发财",
                "一万");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertFalse(analysis.isAlreadyWinning());
        assertEquals(List.of("白板"), analysis.getWinningTiles());
    }

    @Test
    void should_detect_winning_with_one_assumed_meld() {
        // 剩余 11 张：123m 456m 789m 东东（约 1 组碰/杠未录入）
        List<String> tiles = List.of(
                "一万", "二万", "三万",
                "四万", "五万", "六万",
                "七万", "八万", "九万",
                "东风", "东风");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertTrue(analysis.isValid());
        assertTrue(analysis.isAlreadyWinning());
        assertEquals("标准型", analysis.getHandForm());
        assertTrue(analysis.getSuggestion().contains("胡了"));
    }

    @Test
    void should_detect_waiting_with_one_assumed_meld() {
        // 剩余 10 张：123m 456m 789m 东 => 听东
        List<String> tiles = List.of(
                "一万", "二万", "三万",
                "四万", "五万", "六万",
                "七万", "八万", "九万",
                "东风");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertTrue(analysis.isValid());
        assertFalse(analysis.isAlreadyWinning());
        assertEquals(List.of("东风"), analysis.getWinningTiles());
    }

    @Test
    void should_detect_winning_with_two_assumed_melds() {
        // 剩余 8 张：123m 456m 东东
        List<String> tiles = List.of(
                "一万", "二万", "三万",
                "四万", "五万", "六万",
                "东风", "东风");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertTrue(analysis.isAlreadyWinning());
        assertEquals("标准型", analysis.getHandForm());
    }

    @Test
    void should_detect_waiting_with_two_assumed_melds() {
        // 剩余 7 张：123m 456m 东 => 听东
        List<String> tiles = List.of(
                "一万", "二万", "三万",
                "四万", "五万", "六万",
                "东风");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertFalse(analysis.isAlreadyWinning());
        assertEquals(List.of("东风"), analysis.getWinningTiles());
    }

    @Test
    void should_mark_fourteen_winning_with_hule_hint() {
        List<String> tiles = List.of(
                "一万", "二万", "三万",
                "四万", "五万", "六万",
                "七万", "八万", "九万",
                "一筒", "二筒", "三筒",
                "东风", "东风");

        MahjongHandAnalysis analysis = analyzer.analyze(tiles);

        assertTrue(analysis.isAlreadyWinning());
        assertTrue(analysis.getSuggestion().contains("胡了"));
    }

}
