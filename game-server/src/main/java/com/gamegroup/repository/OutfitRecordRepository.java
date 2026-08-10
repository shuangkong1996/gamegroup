package com.gamegroup.repository;

import com.gamegroup.entity.OutfitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutfitRecordRepository extends JpaRepository<OutfitRecord, Long> {

    Optional<OutfitRecord> findByUserIdAndPlanId(Long userId, Long planId);

    Optional<OutfitRecord> findByUserIdAndRenderTaskId(Long userId, Long renderTaskId);

    List<OutfitRecord> findByUserIdOrderByTargetDateDescIdDesc(Long userId);
}
