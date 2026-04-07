package com.gamegroup.repository;

import com.gamegroup.entity.LedgerRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRecordRepository extends JpaRepository<LedgerRecord, Long> {

    Page<LedgerRecord> findByUserIdAndRecordTimeBetweenOrderByRecordTimeDescIdDesc(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    List<LedgerRecord> findByUserIdAndRecordTimeBetweenOrderByRecordTimeAscIdAsc(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    boolean existsByRecurringRuleIdAndRecurringMonth(Long recurringRuleId, String recurringMonth);

    Page<LedgerRecord> findByUserIdAndRecordTimeBetweenAndAccountIdOrderByRecordTimeDescIdDesc(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long accountId,
            Pageable pageable
    );

    Page<LedgerRecord> findByUserIdAndRecordTimeBetweenAndCategoryIdOrderByRecordTimeDescIdDesc(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long categoryId,
            Pageable pageable
    );

    Page<LedgerRecord> findByUserIdAndRecordTimeBetweenAndAccountIdAndCategoryIdOrderByRecordTimeDescIdDesc(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long accountId,
            Long categoryId,
            Pageable pageable
    );

    Optional<LedgerRecord> findByIdAndUserId(Long id, Long userId);
}
