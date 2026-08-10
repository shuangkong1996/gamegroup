package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.AdoptOutfitRenderTaskRequest;
import com.gamegroup.dto.CreateOutfitRenderTaskRequest;
import com.gamegroup.dto.OutfitRenderTaskDTO;
import com.gamegroup.service.OutfitRenderTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "穿搭参考")
@RestController
@RequestMapping("/outfit-renders")
public class OutfitRenderController {

    private final OutfitRenderTaskService outfitRenderTaskService;

    public OutfitRenderController(OutfitRenderTaskService outfitRenderTaskService) {
        this.outfitRenderTaskService = outfitRenderTaskService;
    }

    @Operation(summary = "创建穿搭参考任务")
    @PostMapping("/tasks")
    public ApiResponse<OutfitRenderTaskDTO> createTask(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateOutfitRenderTaskRequest request) {
        return ApiResponse.success(outfitRenderTaskService.createTask(userId, request));
    }

    @Operation(summary = "获取最近参考任务")
    @GetMapping("/tasks")
    public ApiResponse<List<OutfitRenderTaskDTO>> getTasks(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(outfitRenderTaskService.getRecentTasks(userId));
    }

    @Operation(summary = "获取最近一条参考任务")
    @GetMapping("/tasks/latest")
    public ApiResponse<OutfitRenderTaskDTO> getLatestTask(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(outfitRenderTaskService.getLatestTask(userId));
    }

    @Operation(summary = "获取参考任务详情")
    @GetMapping("/tasks/{id}")
    public ApiResponse<OutfitRenderTaskDTO> getTask(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long taskId) {
        return ApiResponse.success(outfitRenderTaskService.getTask(userId, taskId));
    }

    @Operation(summary = "采纳参考搭配")
    @PostMapping("/tasks/{id}/adopt")
    public ApiResponse<Void> adoptTask(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long taskId,
            @RequestBody(required = false) AdoptOutfitRenderTaskRequest request) {
        outfitRenderTaskService.adoptTask(userId, taskId, request == null ? new AdoptOutfitRenderTaskRequest() : request);
        return ApiResponse.success();
    }
}
