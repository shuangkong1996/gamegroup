package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.AnimeGenerateRequest;
import com.gamegroup.dto.AnimeGenerateResponse;
import com.gamegroup.service.HunyuanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI 相关接口")
public class HunyuanController {

    private final HunyuanService hunyuanService;

    @Operation(summary = "生成动漫形象")
    @PostMapping("/anime-generate")
    public ApiResponse<AnimeGenerateResponse> generateAnime(@Valid @RequestBody AnimeGenerateRequest request) {
        // 图片已通过文件上传接口上传，此处接收的是 URL
        // 具体的图片大小限制已在上传接口或前端处理
        
        AnimeGenerateResponse response = hunyuanService.generateAnimeImage(request);
        return ApiResponse.success(response);
    }
}
