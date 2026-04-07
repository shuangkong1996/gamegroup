package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.CreateRoomRequest;
import com.gamegroup.dto.JoinRoomRequest;
import com.gamegroup.dto.MatchRoomDTO;
import com.gamegroup.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "匹配接口")
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "创建匹配房间")
    @PostMapping("/create")
    public ApiResponse<MatchRoomDTO> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @RequestAttribute("userId") Long userId) {
        MatchRoomDTO room = matchService.createRoom(request.getGameId(), userId);
        return ApiResponse.success(room);
    }

    @Operation(summary = "加入匹配房间")
    @PostMapping("/join")
    public ApiResponse<MatchRoomDTO> joinRoom(
            @Valid @RequestBody JoinRoomRequest request,
            @RequestAttribute("userId") Long userId) {
        MatchRoomDTO room = matchService.joinRoom(request.getVerifyCode(), userId);
        return ApiResponse.success(room);
    }

    @Operation(summary = "查询房间状态")
    @GetMapping("/status/{roomId}")
    public ApiResponse<MatchRoomDTO> getRoomStatus(@PathVariable Long roomId) {
        MatchRoomDTO room = matchService.getRoomStatus(roomId);
        return ApiResponse.success(room);
    }

    @Operation(summary = "取消匹配")
    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> cancelRoom(
            @PathVariable Long roomId,
            @RequestAttribute("userId") Long userId) {
        matchService.cancelRoom(roomId, userId);
        return ApiResponse.success();
    }
}
