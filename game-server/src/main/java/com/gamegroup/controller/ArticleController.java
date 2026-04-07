package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.ArticleDTO;
import com.gamegroup.dto.CreateArticleRequest;
import com.gamegroup.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Blog Article", description = "博客文章接口")
@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @Operation(summary = "获取文章列表")
    @GetMapping
    public ApiResponse<Page<ArticleDTO>> getArticles(
            @RequestParam(required = false) Integer status,
            @PageableDefault(sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(articleService.getArticles(status, pageable));
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/{id}")
    public ApiResponse<ArticleDTO> getArticle(@PathVariable Long id) {
        return ApiResponse.success(articleService.getArticle(id));
    }

    @Operation(summary = "发布文章")
    @PostMapping
    public ApiResponse<Long> createArticle(@Validated @RequestBody CreateArticleRequest request) {
        return ApiResponse.success(articleService.createArticle(request));
    }
    
    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ApiResponse.success(null);
    }
}
