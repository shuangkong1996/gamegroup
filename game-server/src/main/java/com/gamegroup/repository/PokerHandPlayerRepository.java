package com.gamegroup.repository;

import com.gamegroup.entity.PokerHandPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PokerHandPlayerRepository extends JpaRepository<PokerHandPlayer, Long> {

    List<PokerHandPlayer> findByHandId(Long handId);

    List<PokerHandPlayer> findByUserId(Long userId);
}
