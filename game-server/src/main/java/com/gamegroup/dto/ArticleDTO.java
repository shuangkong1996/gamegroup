package com.gamegroup.dto;

import java.time.LocalDateTime;

public record ArticleDTO(
    Long id,
    String title,
    String summary,
    String content,
    String coverUrl,
    Integer status,
    Integer views,
    String category,
    LocalDateTime createTime
) {}
