package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.MatchRoomDTO;
import com.gamegroup.entity.Game;
import com.gamegroup.entity.MatchRoom;
import com.gamegroup.repository.GameRepository;
import com.gamegroup.repository.MatchRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRoomRepository matchRoomRepository;
    private final GameRepository gameRepository;

    private static final int ROOM_EXPIRE_MINUTES = 10;

    @Transactional
    public MatchRoomDTO createRoom(Long gameId, Long creatorId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (!game.getStatus()) {
            throw new BusinessException(ErrorCode.GAME_DISABLED);
        }

        MatchRoom room = new MatchRoom();
        room.setGameId(gameId);
        room.setVerifyCode(generateVerifyCode());
        room.setCreatorId(creatorId);
        room.setPlayerIds(creatorId.toString());
        room.setCurrentPlayers(1);
        room.setMaxPlayers(game.getMaxPlayers());
        room.setStatus(MatchRoom.RoomStatus.WAITING);
        room.setExpireTime(LocalDateTime.now().plusMinutes(ROOM_EXPIRE_MINUTES));

        room = matchRoomRepository.save(room);
        log.info("Room created: roomId={}, gameId={}, verifyCode={}", room.getId(), gameId, room.getVerifyCode());

        return toDTO(room, game.getName());
    }

    @Transactional
    public MatchRoomDTO joinRoom(String verifyCode, Long userId) {
        MatchRoom room = matchRoomRepository.findByVerifyCodeAndStatus(verifyCode, MatchRoom.RoomStatus.WAITING)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFY_CODE_INVALID));

        if (room.getExpireTime().isBefore(LocalDateTime.now())) {
            room.setStatus(MatchRoom.RoomStatus.EXPIRED);
            matchRoomRepository.save(room);
            throw new BusinessException(ErrorCode.ROOM_EXPIRED);
        }

        List<Long> playerIds = parsePlayerIds(room.getPlayerIds());
        if (playerIds.contains(userId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_ROOM);
        }

        if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        playerIds.add(userId);
        room.setPlayerIds(playerIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        room.setCurrentPlayers(playerIds.size());

        if (room.getCurrentPlayers().equals(room.getMaxPlayers())) {
            room.setStatus(MatchRoom.RoomStatus.MATCHED);
            log.info("Room matched: roomId={}, players={}", room.getId(), room.getPlayerIds());
        }

        room = matchRoomRepository.save(room);

        Game game = gameRepository.findById(room.getGameId()).orElse(null);
        String gameName = game != null ? game.getName() : "";

        return toDTO(room, gameName);
    }

    public MatchRoomDTO getRoomStatus(Long roomId) {
        MatchRoom room = matchRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        Game game = gameRepository.findById(room.getGameId()).orElse(null);
        String gameName = game != null ? game.getName() : "";

        return toDTO(room, gameName);
    }

    @Transactional
    public void cancelRoom(Long roomId, Long userId) {
        MatchRoom room = matchRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        room.setStatus(MatchRoom.RoomStatus.EXPIRED);
        matchRoomRepository.save(room);
        log.info("Room cancelled: roomId={}", roomId);
    }

    private String generateVerifyCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private List<Long> parsePlayerIds(String playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return Arrays.stream(playerIds.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private MatchRoomDTO toDTO(MatchRoom room, String gameName) {
        return MatchRoomDTO.builder()
                .id(room.getId())
                .gameId(room.getGameId())
                .gameName(gameName)
                .verifyCode(room.getVerifyCode())
                .playerIds(parsePlayerIds(room.getPlayerIds()))
                .currentPlayers(room.getCurrentPlayers())
                .maxPlayers(room.getMaxPlayers())
                .status(room.getStatus().name())
                .expireTime(room.getExpireTime())
                .build();
    }
}
