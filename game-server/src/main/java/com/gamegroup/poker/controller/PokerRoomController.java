package com.gamegroup.poker.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.CreatePokerRoomRequest;
import com.gamegroup.dto.JoinPokerRoomRequest;
import com.gamegroup.dto.PokerHandHistoryDTO;
import com.gamegroup.dto.PokerRoomDTO;
import com.gamegroup.poker.service.PokerRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "扑克房间接口")
@RestController
@RequestMapping("/poker/rooms")
@RequiredArgsConstructor
public class PokerRoomController {

    private final PokerRoomService pokerRoomService;

    @Operation(summary = "创建扑克房间")
    @PostMapping
    public ApiResponse<PokerRoomDTO> createRoom(
            @Valid @RequestBody CreatePokerRoomRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(pokerRoomService.createRoom(request, userId));
    }

    @Operation(summary = "房间列表")
    @GetMapping
    public ApiResponse<List<PokerRoomDTO>> listRooms() {
        return ApiResponse.success(pokerRoomService.listRooms());
    }

    @Operation(summary = "按房号加入")
    @PostMapping("/join")
    public ApiResponse<PokerRoomDTO> joinRoom(
            @Valid @RequestBody JoinPokerRoomRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(pokerRoomService.joinRoom(request.getRoomNo(), userId));
    }

    @Operation(summary = "房间详情")
    @GetMapping("/{roomId}")
    public ApiResponse<PokerRoomDTO> getRoom(@PathVariable Long roomId) {
        return ApiResponse.success(pokerRoomService.getRoom(roomId));
    }

    @Operation(summary = "房间对局历史")
    @GetMapping("/{roomId}/hands")
    public ApiResponse<List<PokerHandHistoryDTO>> listHands(@PathVariable Long roomId) {
        return ApiResponse.success(pokerRoomService.listHands(roomId));
    }

    @Operation(summary = "解散房间")
    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> dissolveRoom(
            @PathVariable Long roomId,
            @RequestAttribute("userId") Long userId) {
        pokerRoomService.dissolveRoom(roomId, userId);
        return ApiResponse.success();
    }
}
