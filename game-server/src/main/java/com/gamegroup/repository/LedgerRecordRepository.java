package com.gamegroup.repository;

import com.gamegroup.entity.LedgerRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("select r from LedgerRecord r where r.userId = :userId "
            + "and r.recordTime >= :startTime and r.recordTime < :endTime "
            + "and (:accountId is null or r.accountId = :accountId) "
            + "and (:categoryId is null or r.categoryId = :categoryId) "
            + "order by r.recordTime asc, r.id asc")
    List<LedgerRecord> findForStats(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId
    );
}
