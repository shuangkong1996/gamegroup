package com.gamegroup.repository;

import com.gamegroup.entity.PokerHandAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PokerHandActionRepository extends JpaRepository<PokerHandAction, Long> {

    List<PokerHandAction> findByHandIdOrderByIdAsc(Long handId);
}
