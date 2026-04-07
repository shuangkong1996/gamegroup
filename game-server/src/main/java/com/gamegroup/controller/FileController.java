package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/file")
@RequiredArgsConstructor
@Tag(name = "File", description = "文件相关接口")
public class FileController {

    private final MinioService minioService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = minioService.uploadFile(file);
        // 获取可访问 URL (开发环境可能需要注意 localhost 问题)
        String url = minioService.getFileUrl(fileName);
        
        Map<String, String> result = new HashMap<>();
        result.put("fileName", fileName);
        result.put("url", url);
        
        return ApiResponse.success(result);
    }
}
