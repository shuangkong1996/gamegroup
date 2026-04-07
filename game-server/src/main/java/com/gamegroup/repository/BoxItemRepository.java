package com.gamegroup.repository;

import com.gamegroup.entity.BoxItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoxItemRepository extends JpaRepository<BoxItem, Long> {

    List<BoxItem> findByBoxId(Long boxId);
}
