package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.GameDTO;
import com.gamegroup.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "游戏接口")
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(summary = "获取游戏列表")
    @GetMapping
    public ApiResponse<List<GameDTO>> getGames() {
        List<GameDTO> games = gameService.getActiveGames();
        return ApiResponse.success(games);
    }

    @Operation(summary = "获取游戏详情")
    @GetMapping("/{id}")
    public ApiResponse<GameDTO> getGame(@PathVariable Long id) {
        GameDTO game = gameService.getGameById(id);
        return ApiResponse.success(game);
    }
}
