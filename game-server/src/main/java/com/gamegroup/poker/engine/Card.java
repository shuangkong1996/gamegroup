package com.gamegroup.poker.engine;

/**
 * 单张扑克牌（纯 Java，不依赖任何框架）。
 * rank: 2..14（11=J, 12=Q, 13=K, 14=A）
 * suit: 'S'=黑桃, 'H'=红桃, 'D'=方块, 'C'=梅花
 * 字符串编码示例："AS"（黑桃A）、"TH"（红桃10）、"2C"（梅花2）。
 */
public record Card(int rank, char suit) implements Comparable<Card> {

    public static final int MIN_RANK = 2;
    public static final int MAX_RANK = 14;

    public Card {
        if (rank < MIN_RANK || rank > MAX_RANK) {
            throw new IllegalArgumentException("非法牌面大小: " + rank);
        }
        if ("SHDC".indexOf(suit) < 0) {
            throw new IllegalArgumentException("非法花色: " + suit);
        }
    }

    /**
     * 解析紧凑编码，如 "AS"、"TH"、"2C"。
     */
    public static Card parse(String code) {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("非法牌编码: " + code);
        }
        char rankChar = Character.toUpperCase(code.charAt(0));
        char suit = Character.toUpperCase(code.charAt(1));
        int rank = switch (rankChar) {
            case 'A' -> 14;
            case 'K' -> 13;
            case 'Q' -> 12;
            case 'J' -> 11;
            case 'T' -> 10;
            default -> {
                if (rankChar >= '2' && rankChar <= '9') {
                    yield rankChar - '0';
                }
                throw new IllegalArgumentException("非法牌面字符: " + rankChar);
            }
        };
        return new Card(rank, suit);
    }

    @Override
    public String toString() {
        char rankChar = switch (rank) {
            case 14 -> 'A';
            case 13 -> 'K';
            case 12 -> 'Q';
            case 11 -> 'J';
            case 10 -> 'T';
            default -> (char) ('0' + rank);
        };
        return "" + rankChar + suit;
    }

    @Override
    public int compareTo(Card other) {
        int byRank = Integer.compare(rank, other.rank);
        if (byRank != 0) {
            return byRank;
        }
        return Character.compare(suit, other.suit);
    }
}
