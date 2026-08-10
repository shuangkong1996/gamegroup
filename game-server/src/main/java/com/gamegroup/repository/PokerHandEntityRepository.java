package com.gamegroup.repository;

import com.gamegroup.entity.PokerHandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PokerHandEntityRepository extends JpaRepository<PokerHandEntity, Long> {

    List<PokerHandEntity> findByRoomIdOrderByHandNoDesc(Long roomId);
}
