package com.gamegroup.repository;

import com.gamegroup.entity.PokerRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PokerRoomRepository extends JpaRepository<PokerRoom, Long> {

    Optional<PokerRoom> findByRoomNo(String roomNo);

    List<PokerRoom> findAllByOrderByCreateTimeDesc();
}
