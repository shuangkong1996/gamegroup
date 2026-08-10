package com.gamegroup.service.mahjong;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MahjongTileCodecTest {

    @Test
    void should_normalize_common_aliases() {
        assertEquals("一万", MahjongTileCodec.normalizeName("1万"));
        assertEquals("五筒", MahjongTileCodec.normalizeName("5饼"));
        assertEquals("一条", MahjongTileCodec.normalizeName("幺鸡"));
        assertEquals("红中", MahjongTileCodec.normalizeName("中"));
        assertEquals("东风", MahjongTileCodec.normalizeName("东"));
        assertEquals("白板", MahjongTileCodec.normalizeName("白"));
    }

    @Test
    void should_sort_tiles_in_standard_order() {
        List<String> sorted = MahjongTileCodec.normalizeAndSort(List.of("东风", "二万", "一万", "红中"));
        assertEquals(List.of("一万", "二万", "东风", "红中"), sorted);
    }

    @Test
    void should_count_tiles() {
        int[] counts = MahjongTileCodec.toCounts(List.of("一万", "一万", "二筒"));
        assertEquals(2, counts[0]);
        assertEquals(1, counts[10]);
        assertEquals(3, MahjongTileCodec.totalTiles(counts));
        assertTrue(!MahjongTileCodec.hasOverflow(counts));
    }
}
