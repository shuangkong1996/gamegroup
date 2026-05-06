package com.gamegroup.repository;

import com.gamegroup.entity.Community;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    @Query("""
            SELECT c
            FROM Community c
            WHERE c.status = 1
              AND (:city IS NULL OR :city = '' OR c.city = :city)
              AND (:keyword IS NULL OR :keyword = '' OR c.name LIKE CONCAT('%', :keyword, '%'))
            ORDER BY c.listingAvgPrice DESC, c.id ASC
            """)
    List<Community> search(
            @Param("keyword") String keyword,
            @Param("city") String city,
            org.springframework.data.domain.Pageable pageable);

    List<Community> findByIdInAndStatus(List<Long> ids, Integer status);
}
