package com.gamegroup.repository;

import com.gamegroup.entity.WardrobeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WardrobeItemRepository extends JpaRepository<WardrobeItem, Long> {

    Optional<WardrobeItem> findByIdAndUserId(Long id, Long userId);

    List<WardrobeItem> findByUserIdAndStatusOrderByIdDesc(Long userId, String status);

    List<WardrobeItem> findByUserIdAndStatusAndRecognizeStatusOrderByIdDesc(Long userId, String status, String recognizeStatus);

    List<WardrobeItem> findByUserIdAndStatusAndRecognizeStatusInOrderByIdDesc(Long userId, String status, Collection<String> recognizeStatuses);

    long countByUserIdAndStatus(Long userId, String status);

    long countByUserIdAndStatusAndRecognizeStatus(Long userId, String status, String recognizeStatus);
}
