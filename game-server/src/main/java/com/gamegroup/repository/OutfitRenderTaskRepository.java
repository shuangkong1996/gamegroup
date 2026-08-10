package com.gamegroup.repository;

import com.gamegroup.entity.OutfitRenderTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutfitRenderTaskRepository extends JpaRepository<OutfitRenderTask, Long> {

    Optional<OutfitRenderTask> findByIdAndUserId(Long id, Long userId);

    List<OutfitRenderTask> findTop10ByUserIdOrderByIdDesc(Long userId);

    Optional<OutfitRenderTask> findFirstByUserIdOrderByIdDesc(Long userId);
}
