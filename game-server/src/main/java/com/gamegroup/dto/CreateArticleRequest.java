package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateArticleRequest(
    @NotBlank(message = "Title cannot be empty")
    String title,
    
    String summary,
    
    @NotBlank(message = "Content cannot be empty")
    String content,
    
    String coverUrl,
    
    @NotNull(message = "Status is required")
    Integer status, // 0: Draft, 1: Published
    
    String category
) {}
