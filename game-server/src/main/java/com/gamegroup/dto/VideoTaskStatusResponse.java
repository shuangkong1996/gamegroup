package com.gamegroup.dto;

/**
 * 即梦AI 视频生成任务状态查询响应。
 *
 * @param taskId   任务 ID
 * @param status   状态：pending / running / success / fail
 * @param videoUrl 成功时的视频 URL；未完成或失败时为 null
 * @param message  失败时的错误信息；成功或进行中时为 null
 */
public record VideoTaskStatusResponse(
        String taskId,
        String status,
        String videoUrl,
        String message
) {}
