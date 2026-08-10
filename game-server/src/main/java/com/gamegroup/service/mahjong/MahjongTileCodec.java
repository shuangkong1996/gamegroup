package com.gamegroup.service.mahjong;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 麻将牌名编解码与标准化。
 * 内部统一使用 0-33 索引：万 0-8、筒 9-17、条 18-26、字 27-33。
 */
public final class MahjongTileCodec {

    public static final int TILE_TYPE_COUNT = 34;
    public static final int MAX_SAME_TILE = 4;

    private static final String[] STANDARD_NAMES = {
            "一万", "二万", "三万", "四万", "五万", "六万", "七万", "八万", "九万",
            "一筒", "二筒", "三筒", "四筒", "五筒", "六筒", "七筒", "八筒", "九筒",
            "一条", "二条", "三条", "四条", "五条", "六条", "七条", "八条", "九条",
            "东风", "南风", "西风", "北风", "红中", "发财", "白板"
    };

    private static final Map<String, Integer> ALIAS_TO_INDEX = buildAliasMap();

    private MahjongTileCodec() {
    }

    public static List<String> standardNames() {
        return List.of(STANDARD_NAMES);
    }

    public static String nameOf(int index) {
        if (index < 0 || index >= TILE_TYPE_COUNT) {
            throw new IllegalArgumentException("非法牌索引: " + index);
        }
        return STANDARD_NAMES[index];
    }

    public static Optional<Integer> indexOf(String rawName) {
        if (rawName == null) {
            return Optional.empty();
        }
        String normalized = normalizeRaw(rawName);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ALIAS_TO_INDEX.get(normalized));
    }

    public static int[] toCounts(List<String> tileNames) {
        int[] counts = new int[TILE_TYPE_COUNT];
        if (tileNames == null) {
            return counts;
        }
        for (String tileName : tileNames) {
            Optional<Integer> index = indexOf(tileName);
            if (index.isEmpty()) {
                throw new IllegalArgumentException("无法识别的牌名: " + tileName);
            }
            counts[index.get()]++;
        }
        return counts;
    }

    public static List<String> toSortedNames(int[] counts) {
        List<String> names = new ArrayList<>();
        if (counts == null) {
            return names;
        }
        for (int i = 0; i < TILE_TYPE_COUNT && i < counts.length; i++) {
            for (int j = 0; j < counts[i]; j++) {
                names.add(STANDARD_NAMES[i]);
            }
        }
        return names;
    }

    public static List<String> normalizeAndSort(List<String> tileNames) {
        int[] counts = toCounts(tileNames);
        return toSortedNames(counts);
    }

    public static String normalizeName(String rawName) {
        return indexOf(rawName).map(MahjongTileCodec::nameOf).orElse(null);
    }

    public static int totalTiles(int[] counts) {
        int total = 0;
        if (counts == null) {
            return 0;
        }
        for (int count : counts) {
            total += count;
        }
        return total;
    }

    public static boolean hasOverflow(int[] counts) {
        if (counts == null) {
            return false;
        }
        for (int count : counts) {
            if (count > MAX_SAME_TILE) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeRaw(String rawName) {
        String value = rawName.replaceAll("\\s+", "")
                .replace("　", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return value
                .replace("條", "条")
                .replace("索", "条")
                .replace("饼", "筒")
                .replace("餅", "筒")
                .replace("筒子", "筒")
                .replace("万子", "万")
                .replace("萬", "万")
                .replace("条子", "条");
    }

    private static Map<String, Integer> buildAliasMap() {
        Map<String, Integer> aliases = new HashMap<>();
        for (int i = 0; i < STANDARD_NAMES.length; i++) {
            aliases.put(STANDARD_NAMES[i], i);
        }

        String[] numbers = {"一", "二", "三", "四", "五", "六", "七", "八", "九"};
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
        String[] suits = {"万", "筒", "条"};
        String[] suitCodes = {"m", "p", "s"};

        for (int suit = 0; suit < suits.length; suit++) {
            for (int rank = 0; rank < 9; rank++) {
                int index = suit * 9 + rank;
                String suitName = suits[suit];
                aliases.put(digits[rank] + suitName, index);
                aliases.put(numbers[rank] + suitName, index);
                aliases.put(digits[rank] + suitCodes[suit], index);
                aliases.put(numbers[rank] + suitCodes[suit], index);
            }
        }

        // 常见别名
        aliases.put("幺鸡", indexOfRequired("一条"));
        aliases.put("幺雞", indexOfRequired("一条"));
        aliases.put("鸡", indexOfRequired("一条"));
        aliases.put("東", indexOfRequired("东风"));
        aliases.put("東风", indexOfRequired("东风"));
        aliases.put("东", indexOfRequired("东风"));
        aliases.put("南", indexOfRequired("南风"));
        aliases.put("西", indexOfRequired("西风"));
        aliases.put("北", indexOfRequired("北风"));
        aliases.put("中", indexOfRequired("红中"));
        aliases.put("红中", indexOfRequired("红中"));
        aliases.put("發", indexOfRequired("发财"));
        aliases.put("发", indexOfRequired("发财"));
        aliases.put("发财", indexOfRequired("发财"));
        aliases.put("白", indexOfRequired("白板"));
        aliases.put("白板", indexOfRequired("白板"));
        aliases.put("1z", indexOfRequired("东风"));
        aliases.put("2z", indexOfRequired("南风"));
        aliases.put("3z", indexOfRequired("西风"));
        aliases.put("4z", indexOfRequired("北风"));
        aliases.put("5z", indexOfRequired("红中"));
        aliases.put("6z", indexOfRequired("发财"));
        aliases.put("7z", indexOfRequired("白板"));
        aliases.put("dong", indexOfRequired("东风"));
        aliases.put("nan", indexOfRequired("南风"));
        aliases.put("xi", indexOfRequired("西风"));
        aliases.put("bei", indexOfRequired("北风"));
        aliases.put("zhong", indexOfRequired("红中"));
        aliases.put("fa", indexOfRequired("发财"));
        aliases.put("bai", indexOfRequired("白板"));

        return Collections.unmodifiableMap(aliases);
    }

    private static int indexOfRequired(String name) {
        for (int i = 0; i < STANDARD_NAMES.length; i++) {
            if (STANDARD_NAMES[i].equals(name)) {
                return i;
            }
        }
        throw new IllegalStateException("标准牌名缺失: " + name);
    }
}
