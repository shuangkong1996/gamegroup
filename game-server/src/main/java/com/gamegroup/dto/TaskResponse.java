package com.gamegroup.dto;

import java.io.Serializable;

public record TaskResponse(
        String taskId,
        String status,
        String videoUrl,
        String coverUrl,
        String failReason
) implements Serializable {}
