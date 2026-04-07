package com.gamegroup.repository;

import com.gamegroup.entity.LedgerCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerCategoryRepository extends JpaRepository<LedgerCategory, Long> {

    List<LedgerCategory> findByUserIdAndTypeOrderBySortOrderAscIdAsc(Long userId, Integer type);

    List<LedgerCategory> findByUserIdInAndTypeOrderBySortOrderAscIdAsc(List<Long> userIds, Integer type);

    Optional<LedgerCategory> findByIdAndUserIdIn(Long id, List<Long> userIds);
}
