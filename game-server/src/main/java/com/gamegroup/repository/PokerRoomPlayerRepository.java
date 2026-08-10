package com.gamegroup.repository;

import com.gamegroup.entity.PokerRoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PokerRoomPlayerRepository extends JpaRepository<PokerRoomPlayer, Long> {

    List<PokerRoomPlayer> findByRoomId(Long roomId);

    Optional<PokerRoomPlayer> findByRoomIdAndSeat(Long roomId, Integer seat);

    Optional<PokerRoomPlayer> findByRoomIdAndUserId(Long roomId, Long userId);

    void deleteByRoomId(Long roomId);
}
