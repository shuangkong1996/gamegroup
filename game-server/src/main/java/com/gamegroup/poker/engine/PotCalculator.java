package com.gamegroup.poker.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 奖池分配器（纯 Java）：根据各玩家本手总投入按金额分层，切出主池与边池，
 * 每池只分给投入达到该层且未弃牌的玩家；并列赢家平分（余数归最小座位）。
 */
public final class PotCalculator {

    private PotCalculator() {
    }

    /**
     * 单名玩家的本手结算信息。
     *
     * @param seat   座位号（仅用于标识与余数分配顺序）
     * @param bet    本手总投入（&gt;= 0）
     * @param inHand 是否未弃牌（参与摊牌）
     * @param hand   未弃牌玩家的牌型结果；弃牌玩家可传 null
     */
    public record Contributor(int seat, BigDecimal bet, boolean inHand, HandValue hand) {
    }

    /**
     * 计算每个座位最终赢取金额。
     */
    public static Map<Integer, BigDecimal> calculate(List<Contributor> contributors) {
        for (Contributor c : contributors) {
            if (c.inHand() && c.hand() == null) {
                throw new IllegalArgumentException("未弃牌玩家必须有牌型结果: seat=" + c.seat);
            }
        }

        Map<Integer, BigDecimal> payouts = new HashMap<>();
        List<Contributor> active = contributors.stream()
                .filter(c -> c.bet() != null && c.bet().signum() > 0)
                .toList();
        if (active.isEmpty()) {
            return payouts;
        }

        // 按投入金额升序去重，得到各池增量层
        List<BigDecimal> levels = active.stream()
                .map(Contributor::bet)
                .distinct()
                .sorted()
                .toList();

        BigDecimal prev = BigDecimal.ZERO;
        for (BigDecimal level : levels) {
            BigDecimal unit = level.subtract(prev);
            List<Contributor> poolPlayers = active.stream()
                    .filter(c -> c.bet().compareTo(level) >= 0)
                    .toList();
            BigDecimal amount = unit.multiply(BigDecimal.valueOf(poolPlayers.size()));

            List<Contributor> contenders = poolPlayers.stream()
                    .filter(Contributor::inHand)
                    .toList();
            if (contenders.isEmpty()) {
                // 理论不发生（all-in 玩家不可弃牌），兜底均分退回
                distribute(contenders, poolPlayers, amount, payouts);
            } else if (contenders.size() == 1) {
                payouts.merge(contenders.get(0).seat(), amount, BigDecimal::add);
            } else {
                distributeWinners(contenders, amount, payouts);
            }
            prev = level;
        }

        // 未赢钱的玩家也补零，保证返回结果包含所有参与者
        for (Contributor c : contributors) {
            payouts.putIfAbsent(c.seat(), BigDecimal.ZERO);
        }
        return payouts;
    }

    /**
     * 兜底：将奖池均分给参与玩家。
     */
    private static void distribute(List<Contributor> contenders, List<Contributor> poolPlayers,
                                   BigDecimal amount, Map<Integer, BigDecimal> payouts) {
        BigDecimal share = amount.divide(BigDecimal.valueOf(poolPlayers.size()), 2, RoundingMode.FLOOR);
        for (Contributor c : poolPlayers) {
            payouts.merge(c.seat(), share, BigDecimal::add);
        }
    }

    /**
     * 找出该池的最强牌，并列者平分；余数归最小座位。
     */
    private static void distributeWinners(List<Contributor> contenders, BigDecimal amount,
                                          Map<Integer, BigDecimal> payouts) {
        List<Contributor> winners = new ArrayList<>();
        winners.add(contenders.get(0));
        for (int i = 1; i < contenders.size(); i++) {
            int cmp = contenders.get(i).hand().compareTo(winners.get(0).hand());
            if (cmp > 0) {
                winners.clear();
                winners.add(contenders.get(i));
            } else if (cmp == 0) {
                winners.add(contenders.get(i));
            }
        }
        winners.sort(Comparator.comparingInt(Contributor::seat));

        BigDecimal share = amount.divide(BigDecimal.valueOf(winners.size()), 2, RoundingMode.FLOOR);
        BigDecimal remainder = amount.subtract(share.multiply(BigDecimal.valueOf(winners.size())));
        for (int i = 0; i < winners.size(); i++) {
            BigDecimal wShare = (i == 0) ? share.add(remainder) : share;
            payouts.merge(winners.get(i).seat(), wShare, BigDecimal::add);
        }
    }
}
