package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.TaskResponse;
import com.gamegroup.dto.VideoSubmitRequest;
import com.gamegroup.service.JimengVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Jimeng AI Video", description = "即梦AI视频生成接口")
@RestController
@RequestMapping("/video")
public class JimengController {

    private final JimengVideoService jimengVideoService;

    public JimengController(JimengVideoService jimengVideoService) {
        this.jimengVideoService = jimengVideoService;
    }

    @Operation(summary = "提交视频生成任务")
    @PostMapping("/submit")
    public ApiResponse<TaskResponse> submitTask(@Validated @RequestBody VideoSubmitRequest request) {
        TaskResponse response = jimengVideoService.submitTask(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "查询任务状态")
    @GetMapping("/status/{taskId}")
    public ApiResponse<TaskResponse> getTaskStatus(@PathVariable String taskId) {
        TaskResponse response = jimengVideoService.getTaskStatus(taskId);
        return ApiResponse.success(response);
    }
}
