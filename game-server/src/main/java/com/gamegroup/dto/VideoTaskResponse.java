package com.gamegroup.dto;

/**
 * 即梦AI 视频生成任务提交响应，返回 task_id 供后续轮询。
 */
public record VideoTaskResponse(String taskId) {}
