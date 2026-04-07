package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.BoxDetailDTO;
import com.gamegroup.dto.BoxRevealDTO;
import com.gamegroup.service.BoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "箱子接口")
@RestController
@RequestMapping("/box")
public class BoxController {

    private final BoxService boxService;

    public BoxController(BoxService boxService) {
        this.boxService = boxService;
    }

    @Operation(summary = "获取箱子完整信息")
    @GetMapping("/{boxId}")
    public ApiResponse<BoxDetailDTO> getBox(@PathVariable Long boxId) {
        return ApiResponse.success(boxService.getBox(boxId));
    }

    @Operation(summary = "获取箱子揭示信息")
    @GetMapping("/{boxId}/revealed")
    public ApiResponse<BoxRevealDTO> getRevealed(@PathVariable Long boxId) {
        return ApiResponse.success(boxService.getRevealedBox(boxId));
    }
}
