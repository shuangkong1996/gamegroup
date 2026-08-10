package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.MahjongAdviceResponse;
import com.gamegroup.dto.MahjongTilesRequest;
import com.gamegroup.service.MahjongAdviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "麻将提示")
@RestController
@RequestMapping("/mahjong")
public class MahjongController {

    private final MahjongAdviceService mahjongAdviceService;

    public MahjongController(MahjongAdviceService mahjongAdviceService) {
        this.mahjongAdviceService = mahjongAdviceService;
    }

    @Operation(summary = "上传手牌照片并获取胡牌提示")
    @PostMapping("/advice")
    public ApiResponse<MahjongAdviceResponse> analyzePhoto(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(mahjongAdviceService.analyzeUpload(file));
    }

    @Operation(summary = "根据手牌列表计算听胡（跳过识牌，便于校验规则）")
    @PostMapping("/analyze-tiles")
    public ApiResponse<MahjongAdviceResponse> analyzeTiles(
            @Valid @RequestBody MahjongTilesRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(mahjongAdviceService.analyzeTiles(request.getTiles()));
    }
}
