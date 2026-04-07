package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.GameSessionDTO;
import com.gamegroup.entity.GameSession;
import com.gamegroup.entity.MatchRoom;
import com.gamegroup.repository.GameSessionRepository;
import com.gamegroup.repository.MatchRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamePlayService {

    private final GameSessionRepository gameSessionRepository;
    private final MatchRoomRepository matchRoomRepository;

    /**
     * 开始游戏 - 创建游戏会话
     */
    @Transactional
    public GameSessionDTO startGame(Long roomId, Long userId) {
        // 先检查是否已有游戏会话（第二个玩家进入时直接返回）
        GameSession existingSession = gameSessionRepository.findByRoomId(roomId).orElse(null);
        if (existingSession != null) {
            return toDTO(existingSession, userId);
        }

        MatchRoom room = matchRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 只有 MATCHED 状态才能创建新游戏
        if (room.getStatus() != MatchRoom.RoomStatus.MATCHED) {
            throw new BusinessException(400, "房间状态不正确，无法开始游戏");
        }

        // 解析玩家列表
        List<Long> playerIds = parsePlayerIds(room.getPlayerIds());
        if (playerIds.size() < 2) {
            throw new BusinessException(400, "玩家数量不足");
        }

        // 创建游戏会话
        GameSession session = new GameSession();
        session.setRoomId(roomId);
        session.setGameId(room.getGameId());
        session.setPlayer1Id(playerIds.get(0));
        session.setPlayer2Id(playerIds.get(1));

        gameSessionRepository.save(session);

        // 更新房间状态为游戏中
        room.setStatus(MatchRoom.RoomStatus.PLAYING);
        matchRoomRepository.save(room);

        return toDTO(session, userId);
    }

    /**
     * 获取游戏状态
     */
    public GameSessionDTO getGameStatus(Long roomId, Long userId) {
        GameSession session = gameSessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new BusinessException(404, "游戏会话不存在"));

        return toDTO(session, userId);
    }

    /**
     * 玩家出招
     */
    @Transactional
    public GameSessionDTO makeChoice(Long roomId, String choice, Long userId) {
        GameSession session = gameSessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new BusinessException(404, "游戏会话不存在"));

        if (session.getStatus() == GameSession.SessionStatus.FINISHED) {
            throw new BusinessException(400, "游戏已结束");
        }

        boolean isPlayer1 = userId.equals(session.getPlayer1Id());
        boolean isPlayer2 = userId.equals(session.getPlayer2Id());

        if (!isPlayer1 && !isPlayer2) {
            throw new BusinessException(403, "您不是该游戏的玩家");
        }

        // 检查是否已出招
        if (isPlayer1 && session.getPlayer1Choice() != null) {
            throw new BusinessException(400, "您本回合已出招");
        }
        if (isPlayer2 && session.getPlayer2Choice() != null) {
            throw new BusinessException(400, "您本回合已出招");
        }

        // 记录选择
        if (isPlayer1) {
            session.setPlayer1Choice(choice);
        } else {
            session.setPlayer2Choice(choice);
        }

        // 检查是否双方都已出招
        if (session.getPlayer1Choice() != null && session.getPlayer2Choice() != null) {
            resolveRound(session);
        }

        gameSessionRepository.save(session);
        return toDTO(session, userId);
    }

    /**
     * 结算当前回合
     */
    private void resolveRound(GameSession session) {
        String p1Choice = session.getPlayer1Choice();
        String p2Choice = session.getPlayer2Choice();

        int result = judgeWinner(p1Choice, p2Choice);

        if (result > 0) {
            session.setPlayer1Wins(session.getPlayer1Wins() + 1);
        } else if (result < 0) {
            session.setPlayer2Wins(session.getPlayer2Wins() + 1);
        }
        // result == 0 平局，不增加胜场

        // 检查是否有人达到2胜(三局两胜)
        if (session.getPlayer1Wins() >= 2) {
            session.setStatus(GameSession.SessionStatus.FINISHED);
            session.setWinnerId(session.getPlayer1Id());
            updateRoomFinished(session.getRoomId());
        } else if (session.getPlayer2Wins() >= 2) {
            session.setStatus(GameSession.SessionStatus.FINISHED);
            session.setWinnerId(session.getPlayer2Id());
            updateRoomFinished(session.getRoomId());
        } else {
            // 进入下一回合，清空选择
            session.setCurrentRound(session.getCurrentRound() + 1);
            session.setPlayer1Choice(null);
            session.setPlayer2Choice(null);
        }
    }

    /**
     * 判断胜负: 1=玩家1赢, -1=玩家2赢, 0=平局
     */
    private int judgeWinner(String p1, String p2) {
        if (p1.equals(p2)) {
            return 0;
        }
        // ROCK > SCISSORS, SCISSORS > PAPER, PAPER > ROCK
        if ((p1.equals("ROCK") && p2.equals("SCISSORS")) ||
            (p1.equals("SCISSORS") && p2.equals("PAPER")) ||
            (p1.equals("PAPER") && p2.equals("ROCK"))) {
            return 1;
        }
        return -1;
    }

    private void updateRoomFinished(Long roomId) {
        matchRoomRepository.findById(roomId).ifPresent(room -> {
            room.setStatus(MatchRoom.RoomStatus.FINISHED);
            matchRoomRepository.save(room);
        });
    }

    private List<Long> parsePlayerIds(String playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(playerIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }

    /**
     * 转换为 DTO，根据当前用户视角返回信息
     */
    private GameSessionDTO toDTO(GameSession session, Long userId) {
        GameSessionDTO dto = new GameSessionDTO();
        dto.setId(session.getId());
        dto.setRoomId(session.getRoomId());
        dto.setGameId(session.getGameId());
        dto.setPlayer1Id(session.getPlayer1Id());
        dto.setPlayer2Id(session.getPlayer2Id());
        dto.setCurrentRound(session.getCurrentRound());
        dto.setPlayer1Wins(session.getPlayer1Wins());
        dto.setPlayer2Wins(session.getPlayer2Wins());
        dto.setStatus(session.getStatus().name());
        dto.setWinnerId(session.getWinnerId());

        boolean isPlayer1 = userId.equals(session.getPlayer1Id());
        dto.setIsPlayer1(isPlayer1);

        String myChoice = isPlayer1 ? session.getPlayer1Choice() : session.getPlayer2Choice();
        String opponentChoice = isPlayer1 ? session.getPlayer2Choice() : session.getPlayer1Choice();

        dto.setHasChosen(myChoice != null);
        dto.setOpponentHasChosen(opponentChoice != null);
        dto.setMyChoice(myChoice);

        // 只有双方都出招后才显示对手选择和回合结果
        if (myChoice != null && opponentChoice != null) {
            dto.setOpponentChoice(opponentChoice);

            int result = judgeWinner(
                    isPlayer1 ? myChoice : opponentChoice,
                    isPlayer1 ? opponentChoice : myChoice
            );
            if (result == 0) {
                dto.setRoundResult("DRAW");
            } else if ((isPlayer1 && result > 0) || (!isPlayer1 && result < 0)) {
                dto.setRoundResult("WIN");
            } else {
                dto.setRoundResult("LOSE");
            }
        }

        return dto;
    }
}
