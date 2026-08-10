package com.gamegroup.poker.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.PokerStatsDTO;
import com.gamegroup.poker.service.PokerStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "扑克战绩接口")
@RestController
@RequestMapping("/poker/stats")
@RequiredArgsConstructor
public class PokerStatsController {

    private final PokerStatsService pokerStatsService;

    @Operation(summary = "我的战绩")
    @GetMapping("/me")
    public ApiResponse<PokerStatsDTO> myStats(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(pokerStatsService.statsOf(userId));
    }
}
