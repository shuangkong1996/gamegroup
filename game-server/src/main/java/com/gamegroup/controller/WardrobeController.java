package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.UpdateWardrobeItemRequest;
import com.gamegroup.dto.WardrobeCategoryOverviewDTO;
import com.gamegroup.dto.WardrobeItemDTO;
import com.gamegroup.dto.WardrobeItemDetailDTO;
import com.gamegroup.dto.WardrobeOverviewDTO;
import com.gamegroup.dto.WardrobeUploadResponse;
import com.gamegroup.service.WardrobeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "我的衣柜")
@RestController
@RequestMapping("/wardrobe")
@RequiredArgsConstructor
public class WardrobeController {

    private final WardrobeService wardrobeService;

    @Operation(summary = "上传衣物")
    @PostMapping("/items/upload")
    public ApiResponse<WardrobeUploadResponse> uploadItem(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(wardrobeService.uploadItem(userId, file));
    }

    @Operation(summary = "获取衣柜首页总览")
    @GetMapping("/overview")
    public ApiResponse<WardrobeOverviewDTO> getOverview(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(wardrobeService.getOverview(userId));
    }

    @Operation(summary = "获取衣物列表")
    @GetMapping("/items")
    public ApiResponse<List<WardrobeItemDTO>> getItems(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String recognizeStatus) {
        return ApiResponse.success(wardrobeService.getItems(userId, recognizeStatus));
    }

    @Operation(summary = "获取衣物分类总览")
    @GetMapping("/categories")
    public ApiResponse<WardrobeCategoryOverviewDTO> getCategories(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(wardrobeService.getCategoryOverview(userId));
    }

    @Operation(summary = "获取衣物详情")
    @GetMapping("/items/{id}")
    public ApiResponse<WardrobeItemDetailDTO> getItemDetail(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long itemId) {
        return ApiResponse.success(wardrobeService.getItemDetail(userId, itemId));
    }

    @Operation(summary = "更新衣物信息")
    @PutMapping("/items/{id}")
    public ApiResponse<WardrobeItemDetailDTO> updateItem(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long itemId,
            @Valid @RequestBody UpdateWardrobeItemRequest request) {
        return ApiResponse.success(wardrobeService.updateItem(userId, itemId, request));
    }

    @Operation(summary = "归档衣物")
    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> archiveItem(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long itemId) {
        wardrobeService.archiveItem(userId, itemId);
        return ApiResponse.success();
    }
}
