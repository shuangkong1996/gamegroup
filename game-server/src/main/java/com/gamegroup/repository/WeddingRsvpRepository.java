package com.gamegroup.repository;

import com.gamegroup.entity.WeddingRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeddingRsvpRepository extends JpaRepository<WeddingRsvp, Long> {
}
