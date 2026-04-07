package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.AuctionBidRequest;
import com.gamegroup.dto.AuctionDTO;
import com.gamegroup.dto.StartAuctionRequest;
import com.gamegroup.service.AuctionService;
import com.gamegroup.service.BoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "拍卖接口")
@RestController
@RequestMapping("/auction")
public class AuctionController {

    private final AuctionService auctionService;
    private final BoxService boxService;

    public AuctionController(AuctionService auctionService, BoxService boxService) {
        this.auctionService = auctionService;
        this.boxService = boxService;
    }

    @Operation(summary = "创建并开始拍卖")
    @PostMapping("/start")
    public ApiResponse<AuctionDTO> startAuction(@Valid @RequestBody StartAuctionRequest request) {
        Long boxId = boxService.generateBox().getBox().getId();
        AuctionDTO auction = auctionService.startAuction(request.getRoomId(), boxId);
        return ApiResponse.success(auction);
    }

    @Operation(summary = "获取拍卖信息")
    @GetMapping("/{auctionId}")
    public ApiResponse<AuctionDTO> getAuction(@PathVariable Long auctionId) {
        return ApiResponse.success(auctionService.getAuction(auctionId));
    }

    @Operation(summary = "提交出价")
    @PostMapping("/bid")
    public ApiResponse<AuctionDTO> submitBid(
            @Valid @RequestBody AuctionBidRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(auctionService.submitBid(request, userId));
    }

    @Operation(summary = "结算当前轮次")
    @PostMapping("/{auctionId}/settle")
    public ApiResponse<AuctionDTO> settleRound(@PathVariable Long auctionId) {
        return ApiResponse.success(auctionService.settleRound(auctionId));
    }
}
