package com.gamegroup.repository;

import com.gamegroup.entity.OutfitPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitPlanItemRepository extends JpaRepository<OutfitPlanItem, Long> {

    List<OutfitPlanItem> findByPlanIdOrderByIdAsc(Long planId);

    void deleteByPlanId(Long planId);
}
