package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 即梦AI 视频生成任务提交请求。
 * 支持文生视频（仅 prompt）或图生视频（prompt + image_url）。
 */
public record VideoSubmitRequest(
        @NotBlank(message = "prompt 不能为空")
        @Size(max = 2000)
        String prompt,

        /**
         * 图生视频时的首帧图片 URL，可选；不传则为文生视频。
         */
        @Size(max = 2048)
        String imageUrl
) {}
