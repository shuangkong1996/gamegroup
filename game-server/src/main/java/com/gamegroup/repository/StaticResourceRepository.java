package com.gamegroup.repository;

import com.gamegroup.entity.StaticResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaticResourceRepository extends JpaRepository<StaticResource, Long> {

    boolean existsByGroupKeyAndResourceKey(String groupKey, String resourceKey);

    List<StaticResource> findByGroupKeyAndStatusTrueOrderBySortOrderAscIdAsc(String groupKey);
}
