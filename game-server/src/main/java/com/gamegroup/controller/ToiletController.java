package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.StartToiletRequest;
import com.gamegroup.dto.StopToiletResponse;
import com.gamegroup.dto.ToiletRecordDTO;
import com.gamegroup.dto.ToiletSessionDTO;
import com.gamegroup.dto.ToiletStatisticsDTO;
import com.gamegroup.service.ToiletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "厕所时间接口")
@RestController
@RequestMapping("/toilet")
@RequiredArgsConstructor
public class ToiletController {

    private final ToiletService toiletService;

    @Operation(summary = "开始计时")
    @PostMapping("/start")
    public ApiResponse<ToiletSessionDTO> startSession(
            @Valid @RequestBody StartToiletRequest request,
            @RequestAttribute("userId") Long userId) {
        ToiletSessionDTO session = toiletService.startSession(userId, request.getMonthlySalary());
        return ApiResponse.success(session);
    }

    @Operation(summary = "结束计时")
    @PostMapping("/stop")
    public ApiResponse<StopToiletResponse> stopSession(
            @RequestAttribute("userId") Long userId) {
        StopToiletResponse response = toiletService.stopSession(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取当前会话状态")
    @GetMapping("/current")
    public ApiResponse<ToiletSessionDTO> getCurrentSession(
            @RequestAttribute("userId") Long userId) {
        ToiletSessionDTO session = toiletService.getCurrentSession(userId);
        return ApiResponse.success(session);
    }

    @Operation(summary = "获取历史记录")
    @GetMapping("/history")
    public ApiResponse<Page<ToiletRecordDTO>> getHistory(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ToiletRecordDTO> history = toiletService.getHistory(userId, page, size);
        return ApiResponse.success(history);
    }

    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    public ApiResponse<ToiletStatisticsDTO> getStatistics(
            @RequestAttribute("userId") Long userId) {
        ToiletStatisticsDTO statistics = toiletService.getStatistics(userId);
        return ApiResponse.success(statistics);
    }
}
