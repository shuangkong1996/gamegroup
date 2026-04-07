package com.gamegroup.scheduler;

import com.gamegroup.entity.Auction;
import com.gamegroup.repository.AuctionRepository;
import com.gamegroup.service.AuctionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuctionRoundScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    public AuctionRoundScheduler(AuctionRepository auctionRepository, AuctionService auctionService) {
        this.auctionRepository = auctionRepository;
        this.auctionService = auctionService;
    }

    @Scheduled(fixedDelay = 10000)
    public void settleExpiredRounds() {
        List<Auction> auctions = auctionRepository.findAll();
        if (auctions.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : auctions) {
            if (auction.getStatus() != Auction.AuctionStatus.IN_PROGRESS) {
                continue;
            }
            if (auction.getRoundEndTime() == null || auction.getRoundEndTime().isAfter(now)) {
                continue;
            }
            try {
                auctionService.settleRound(auction.getId());
            } catch (Exception e) {
                continue;
            }
        }
    }
}
