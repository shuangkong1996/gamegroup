package com.gamegroup.repository;

import com.gamegroup.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {

    List<AuctionBid> findByAuctionIdAndRound(Long auctionId, Integer round);

    Optional<AuctionBid> findByAuctionIdAndRoundAndUserId(Long auctionId, Integer round, Long userId);
}
