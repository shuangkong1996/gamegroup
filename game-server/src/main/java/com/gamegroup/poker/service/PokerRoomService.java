package com.gamegroup.poker.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.CreatePokerRoomRequest;
import com.gamegroup.dto.PokerHandHistoryDTO;
import com.gamegroup.dto.PokerHandPlayerDTO;
import com.gamegroup.dto.PokerPlayerDTO;
import com.gamegroup.dto.PokerRoomDTO;
import com.gamegroup.entity.PokerHandEntity;
import com.gamegroup.entity.PokerHandPlayer;
import com.gamegroup.entity.PokerRoom;
import com.gamegroup.entity.PokerRoomPlayer;
import com.gamegroup.poker.engine.PokerTable;
import com.gamegroup.repository.PokerHandEntityRepository;
import com.gamegroup.repository.PokerHandPlayerRepository;
import com.gamegroup.repository.PokerRoomPlayerRepository;
import com.gamegroup.repository.PokerRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * 扑克房间管理（REST）：创建、加入、查询、解散。游戏内操作走 WebSocket。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PokerRoomService {

    private final PokerRoomRepository roomRepository;
    private final PokerRoomPlayerRepository playerRepository;
    private final PokerHandEntityRepository handEntityRepository;
    private final PokerHandPlayerRepository handPlayerRepository;
    private final PokerGameService pokerGameService;

    @Transactional
    public PokerRoomDTO createRoom(CreatePokerRoomRequest request, Long userId) {
        if (request.getBuyInMin().compareTo(request.getBuyInMax()) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "最小买入不能大于最大买入");
        }
        PokerRoom room = new PokerRoom();
        room.setRoomNo(generateUniqueRoomNo());
        room.setName(request.getName());
        room.setOwnerId(userId);
        room.setTableSize(request.getTableSize());
        room.setSmallBlind(request.getSmallBlind());
        room.setBigBlind(request.getBigBlind());
        room.setBuyInMin(request.getBuyInMin());
        room.setBuyInMax(request.getBuyInMax());
        room.setThinkSeconds(request.getThinkSeconds());
        room.setStatus("WAITING");
        room.setCurrentPlayers(0);
        room = roomRepository.save(room);

        // 创建房间运行时状态（引擎桌）
        PokerTable table = new PokerTable(
                room.getTableSize(), room.getSmallBlind(), room.getBigBlind());
        pokerGameService.registerRoom(room.getId(), table);

        log.info("创建扑克房间: id={}, roomNo={}, name={}, owner={}", room.getId(), room.getRoomNo(), room.getName(), userId);
        return toDTO(room);
    }

    public List<PokerRoomDTO> listRooms() {
        return roomRepository.findAllByOrderByCreateTimeDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public PokerRoomDTO joinRoom(String roomNo, Long userId) {
        PokerRoom room = roomRepository.findByRoomNo(roomNo.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));
        if (!"WAITING".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.POKER_ROOM_STATUS_INVALID);
        }
        // 已在其他房间入座的用户不能重复加入（简单限制：同一用户一个房间）
        playerRepository.findByRoomIdAndUserId(room.getId(), userId)
                .ifPresent(p -> {
                    if ("SEATED".equals(p.getStatus())) {
                        throw new BusinessException(ErrorCode.ALREADY_IN_ROOM);
                    }
                });
        return toDTO(room);
    }

    public PokerRoomDTO getRoom(Long roomId) {
        PokerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));
        return toDTO(room);
    }

    /**
     * 房间对局历史。
     */
    public List<PokerHandHistoryDTO> listHands(Long roomId) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));
        return handEntityRepository.findByRoomIdOrderByHandNoDesc(roomId).stream()
                .map(hand -> PokerHandHistoryDTO.builder()
                        .id(hand.getId())
                        .handNo(hand.getHandNo())
                        .dealerSeat(hand.getDealerSeat())
                        .boardCards(hand.getBoardCards())
                        .pot(hand.getPot())
                        .endTime(hand.getEndTime())
                        .players(handPlayerRepository.findByHandId(hand.getId()).stream()
                                .map(this::toPlayerDTO)
                                .toList())
                        .build())
                .toList();
    }

    private PokerHandPlayerDTO toPlayerDTO(PokerHandPlayer p) {
        return PokerHandPlayerDTO.builder()
                .seat(p.getSeat())
                .userId(p.getUserId())
                .holeCards(p.getHoleCards())
                .betTotal(p.getBetTotal())
                .handRank(p.getHandRank())
                .result(p.getResult())
                .winAmount(p.getWinAmount())
                .build();
    }

    @Transactional
    public void dissolveRoom(Long roomId, Long userId) {
        PokerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));
        if (!room.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        playerRepository.deleteByRoomId(roomId);
        roomRepository.delete(room);
        pokerGameService.closeRoom(roomId);
        log.info("解散扑克房间: id={}", roomId);
    }

    private PokerRoomDTO toDTO(PokerRoom room) {
        List<PokerPlayerDTO> players = playerRepository.findByRoomId(room.getId()).stream()
                .filter(p -> "SEATED".equals(p.getStatus()))
                .map(p -> PokerPlayerDTO.builder()
                        .seat(p.getSeat())
                        .userId(p.getUserId())
                        .nickname(p.getNickname())
                        .avatarUrl(p.getAvatarUrl())
                        .chip(p.getChip())
                        .build())
                .toList();
        return PokerRoomDTO.builder()
                .id(room.getId())
                .roomNo(room.getRoomNo())
                .name(room.getName())
                .ownerId(room.getOwnerId())
                .tableSize(room.getTableSize())
                .smallBlind(room.getSmallBlind())
                .bigBlind(room.getBigBlind())
                .buyInMin(room.getBuyInMin())
                .buyInMax(room.getBuyInMax())
                .thinkSeconds(room.getThinkSeconds())
                .status(room.getStatus())
                .handNo(room.getHandNo())
                .currentPlayers(players.size())
                .players(players)
                .build();
    }

    private String generateUniqueRoomNo() {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                sb.append(random.nextInt(10));
            }
            String no = sb.toString();
            if (roomRepository.findByRoomNo(no).isEmpty()) {
                return no;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
}
