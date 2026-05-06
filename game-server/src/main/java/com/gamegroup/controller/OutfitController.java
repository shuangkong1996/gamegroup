package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.AdoptOutfitRequest;
import com.gamegroup.dto.DailyOutfitDTO;
import com.gamegroup.dto.OutfitRecordDTO;
import com.gamegroup.service.OutfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "搭配建议")
@RestController
@RequestMapping("/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    @Operation(summary = "获取每日搭配")
    @GetMapping("/daily")
    public ApiResponse<DailyOutfitDTO> getDailyOutfit(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String targetDate) {
        return ApiResponse.success(outfitService.getDailyOutfit(userId, targetDate));
    }

    @Operation(summary = "换一组搭配")
    @PostMapping("/daily/refresh")
    public ApiResponse<DailyOutfitDTO> refreshDailyOutfit(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String targetDate) {
        return ApiResponse.success(outfitService.refreshDailyOutfit(userId, targetDate));
    }

    @Operation(summary = "采纳搭配")
    @PostMapping("/{planId}/adopt")
    public ApiResponse<Void> adoptOutfit(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long planId,
            @RequestBody(required = false) AdoptOutfitRequest request) {
        outfitService.adoptOutfit(userId, planId, request == null ? new AdoptOutfitRequest() : request);
        return ApiResponse.success();
    }

    @Operation(summary = "获取搭配记录")
    @GetMapping("/records")
    public ApiResponse<List<OutfitRecordDTO>> getRecords(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(outfitService.getRecords(userId));
    }
}
