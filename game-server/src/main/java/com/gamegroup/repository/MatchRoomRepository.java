package com.gamegroup.repository;

import com.gamegroup.entity.MatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchRoomRepository extends JpaRepository<MatchRoom, Long> {

    Optional<MatchRoom> findByVerifyCodeAndStatus(String verifyCode, MatchRoom.RoomStatus status);

    Optional<MatchRoom> findByVerifyCode(String verifyCode);
}
