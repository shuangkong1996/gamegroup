package com.gamegroup.repository;

import com.gamegroup.entity.WardrobeItemDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WardrobeItemDetailRepository extends JpaRepository<WardrobeItemDetail, Long> {

    Optional<WardrobeItemDetail> findByItemId(Long itemId);
}
