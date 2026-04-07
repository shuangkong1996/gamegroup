package com.gamegroup.repository;

import com.gamegroup.entity.LedgerRecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRecurringRuleRepository extends JpaRepository<LedgerRecurringRule, Long> {

    List<LedgerRecurringRule> findByUserIdOrderByIdDesc(Long userId);

    Optional<LedgerRecurringRule> findByIdAndUserId(Long id, Long userId);
}

