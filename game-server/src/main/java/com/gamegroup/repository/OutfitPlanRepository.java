package com.gamegroup.repository;

import com.gamegroup.entity.OutfitPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutfitPlanRepository extends JpaRepository<OutfitPlan, Long> {

    Optional<OutfitPlan> findFirstByUserIdAndTargetDateAndStatusOrderByIdDesc(Long userId, LocalDate targetDate, String status);

    Optional<OutfitPlan> findByIdAndUserId(Long id, Long userId);

    List<OutfitPlan> findByUserIdOrderByTargetDateDescIdDesc(Long userId);
}
