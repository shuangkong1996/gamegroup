package com.gamegroup.poker.service;

import com.gamegroup.dto.PokerStatsDTO;
import com.gamegroup.entity.PokerHandPlayer;
import com.gamegroup.repository.PokerHandPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 玩家战绩统计：基于对局玩家明细聚合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PokerStatsService {

    private final PokerHandPlayerRepository handPlayerRepository;

    public PokerStatsDTO statsOf(Long userId) {
        List<PokerHandPlayer> records = handPlayerRepository.findByUserId(userId);

        int played = records.size();
        int won = (int) records.stream()
                .filter(r -> "WIN".equals(r.getResult()))
                .count();
        BigDecimal totalWin = records.stream()
                .map(PokerHandPlayer::getWinAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBet = records.stream()
                .map(PokerHandPlayer::getBetTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal winRate = played == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(won)
                        .divide(BigDecimal.valueOf(played), 4, RoundingMode.HALF_UP);

        return PokerStatsDTO.builder()
                .userId(userId)
                .handsPlayed(played)
                .handsWon(won)
                .winRate(winRate)
                .totalWin(totalWin)
                .totalBet(totalBet)
                .build();
    }
}
