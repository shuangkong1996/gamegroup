package com.gamegroup.repository;

import com.gamegroup.entity.ToiletSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ToiletSessionRepository extends JpaRepository<ToiletSession, Long> {

    Optional<ToiletSession> findByUserIdAndStatus(Long userId, ToiletSession.SessionStatus status);

    boolean existsByUserIdAndStatus(Long userId, ToiletSession.SessionStatus status);
}
