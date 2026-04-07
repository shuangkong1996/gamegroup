package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.GameSessionDTO;
import com.gamegroup.dto.MakeChoiceRequest;
import com.gamegroup.service.GamePlayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "游戏对战接口")
@RestController
@RequestMapping("/gameplay")
@RequiredArgsConstructor
public class GamePlayController {

    private final GamePlayService gamePlayService;

    @Operation(summary = "开始游戏")
    @PostMapping("/start/{roomId}")
    public ApiResponse<GameSessionDTO> startGame(
            @PathVariable Long roomId,
            @RequestAttribute("userId") Long userId) {
        GameSessionDTO session = gamePlayService.startGame(roomId, userId);
        return ApiResponse.success(session);
    }

    @Operation(summary = "获取游戏状态")
    @GetMapping("/status/{roomId}")
    public ApiResponse<GameSessionDTO> getGameStatus(
            @PathVariable Long roomId,
            @RequestAttribute("userId") Long userId) {
        GameSessionDTO session = gamePlayService.getGameStatus(roomId, userId);
        return ApiResponse.success(session);
    }

    @Operation(summary = "出招")
    @PostMapping("/choice")
    public ApiResponse<GameSessionDTO> makeChoice(
            @Valid @RequestBody MakeChoiceRequest request,
            @RequestAttribute("userId") Long userId) {
        GameSessionDTO session = gamePlayService.makeChoice(
                request.getRoomId(), request.getChoice(), userId);
        return ApiResponse.success(session);
    }
}
