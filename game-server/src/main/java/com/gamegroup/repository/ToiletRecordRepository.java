package com.gamegroup.repository;

import com.gamegroup.entity.ToiletRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ToiletRecordRepository extends JpaRepository<ToiletRecord, Long> {

    Page<ToiletRecord> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.durationSeconds), 0) FROM ToiletRecord r WHERE r.userId = :userId")
    Integer sumDurationSecondsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(r.earnedMoney), 0) FROM ToiletRecord r WHERE r.userId = :userId")
    BigDecimal sumEarnedMoneyByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM ToiletRecord r WHERE r.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
}
