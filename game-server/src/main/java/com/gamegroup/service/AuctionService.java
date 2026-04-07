package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.AuctionBidRequest;
import com.gamegroup.dto.AuctionDTO;
import com.gamegroup.entity.Auction;
import com.gamegroup.entity.AuctionBid;
import com.gamegroup.entity.MatchRoom;
import com.gamegroup.repository.AuctionBidRepository;
import com.gamegroup.repository.AuctionRepository;
import com.gamegroup.repository.BoxRepository;
import com.gamegroup.repository.MatchRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final BoxRepository boxRepository;
    private final MatchRoomRepository matchRoomRepository;

    private static final int MAX_ROUNDS = 5;
    private static final BigDecimal MIN_BID_STEP_RATIO = new BigDecimal("0.05");
    private static final int ROUND_TIMEOUT_SECONDS = 60;
    private static final BigDecimal[] WIN_RATIOS = {
            new BigDecimal("2.00"),
            new BigDecimal("1.80"),
            new BigDecimal("1.50"),
            new BigDecimal("1.20")
    };

    public AuctionService(AuctionRepository auctionRepository,
                          AuctionBidRepository auctionBidRepository,
                          BoxRepository boxRepository,
                          MatchRoomRepository matchRoomRepository) {
        this.auctionRepository = auctionRepository;
        this.auctionBidRepository = auctionBidRepository;
        this.boxRepository = boxRepository;
        this.matchRoomRepository = matchRoomRepository;
    }

    public AuctionDTO getAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));
        return toDTO(auction);
    }

    @Transactional
    public AuctionDTO submitBid(AuctionBidRequest request, Long userId) {
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != Auction.AuctionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.AUCTION_STATUS_INVALID);
        }

        if (auction.getCurrentRound() > MAX_ROUNDS) {
            throw new BusinessException(ErrorCode.BID_ROUND_INVALID);
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BID_AMOUNT_INVALID);
        }

        if (auctionBidRepository.findByAuctionIdAndRoundAndUserId(
                auction.getId(), auction.getCurrentRound(), userId).isPresent()) {
            throw new BusinessException(ErrorCode.BID_ALREADY_SUBMITTED);
        }

        BigDecimal minBid = getMinBidAmount(auction);
        if (request.getAmount().compareTo(minBid) < 0) {
            throw new BusinessException(ErrorCode.BID_AMOUNT_INVALID);
        }

        AuctionBid bid = new AuctionBid();
        bid.setAuctionId(auction.getId());
        bid.setUserId(userId);
        bid.setRound(auction.getCurrentRound());
        bid.setAmount(request.getAmount());
        bid.setStatus(AuctionBid.BidStatus.VALID);
        auctionBidRepository.save(bid);

        return toDTO(auction);
    }

    @Transactional
    public AuctionDTO startAuction(Long roomId, Long boxId) {
        if (!boxRepository.existsById(boxId)) {
            throw new BusinessException(ErrorCode.BOX_NOT_FOUND);
        }

        MatchRoom room = matchRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != MatchRoom.RoomStatus.MATCHED) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }

        Auction auction = auctionRepository.findByRoomId(roomId).orElse(null);
        if (auction != null) {
            return toDTO(auction);
        }

        Auction newAuction = new Auction();
        newAuction.setRoomId(roomId);
        newAuction.setBoxId(boxId);
        newAuction.setStatus(Auction.AuctionStatus.IN_PROGRESS);
        newAuction.setRoundEndTime(LocalDateTime.now().plusSeconds(ROUND_TIMEOUT_SECONDS));
        newAuction = auctionRepository.save(newAuction);

        room.setStatus(MatchRoom.RoomStatus.PLAYING);
        matchRoomRepository.save(room);

        return toDTO(newAuction);
    }

    @Transactional
    public AuctionDTO settleRound(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != Auction.AuctionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.AUCTION_STATUS_INVALID);
        }

        List<AuctionBid> bids = auctionBidRepository.findByAuctionIdAndRound(auctionId, auction.getCurrentRound());
        if (bids.isEmpty()) {
            throw new BusinessException(ErrorCode.AUCTION_NO_BIDS);
        }

        AuctionBid highest = null;
        AuctionBid second = null;
        for (AuctionBid bid : bids) {
            if (highest == null || bid.getAmount().compareTo(highest.getAmount()) > 0) {
                second = highest;
                highest = bid;
            } else if (second == null || bid.getAmount().compareTo(second.getAmount()) > 0) {
                second = bid;
            }
        }

        boolean isFinalRound = auction.getCurrentRound() >= MAX_ROUNDS;
        if (isFinalRound || isWinningBid(auction.getCurrentRound(), highest, second)) {
            auction.setWinnerId(highest.getUserId());
            auction.setFinalPrice(highest.getAmount());
            auction.setStatus(Auction.AuctionStatus.SETTLEMENT);
            auctionRepository.save(auction);
            return toDTO(auction);
        }

        auction.setCurrentRound(auction.getCurrentRound() + 1);
        auction.setRoundEndTime(LocalDateTime.now().plusSeconds(ROUND_TIMEOUT_SECONDS));
        auctionRepository.save(auction);
        return toDTO(auction);
    }

    public List<AuctionBid> getRoundBids(Long auctionId, Integer round) {
        return auctionBidRepository.findByAuctionIdAndRound(auctionId, round);
    }

    private BigDecimal getMinBidAmount(Auction auction) {
        List<AuctionBid> bids = auctionBidRepository.findByAuctionIdAndRound(auction.getId(), auction.getCurrentRound());
        BigDecimal highest = BigDecimal.ZERO;
        for (AuctionBid bid : bids) {
            if (bid.getAmount().compareTo(highest) > 0) {
                highest = bid.getAmount();
            }
        }

        if (highest.compareTo(BigDecimal.ZERO) > 0) {
            return highest.add(highest.multiply(MIN_BID_STEP_RATIO));
        }

        return boxRepository.findById(auction.getBoxId())
                .map(box -> box.getStartingPrice())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOX_NOT_FOUND));
    }

    private boolean isWinningBid(int round, AuctionBid highest, AuctionBid second) {
        if (round >= MAX_ROUNDS) {
            return true;
        }
        if (highest == null || second == null) {
            return false;
        }
        BigDecimal ratio = highest.getAmount().divide(second.getAmount(), 2, java.math.RoundingMode.HALF_UP);
        return ratio.compareTo(WIN_RATIOS[round - 1]) >= 0;
    }

    private AuctionDTO toDTO(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.setId(auction.getId());
        dto.setRoomId(auction.getRoomId());
        dto.setBoxId(auction.getBoxId());
        dto.setCurrentRound(auction.getCurrentRound());
        dto.setStatus(auction.getStatus().name());
        dto.setWinnerId(auction.getWinnerId());
        dto.setFinalPrice(auction.getFinalPrice());
        dto.setRoundEndTime(auction.getRoundEndTime());
        return dto;
    }
}
