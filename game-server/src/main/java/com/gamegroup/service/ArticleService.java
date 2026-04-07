package com.gamegroup.service;

import com.gamegroup.dto.ArticleDTO;
import com.gamegroup.dto.CreateArticleRequest;
import com.gamegroup.entity.Article;
import com.gamegroup.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public Page<ArticleDTO> getArticles(Integer status, Pageable pageable) {
        Page<Article> articles;
        if (status != null) {
            articles = articleRepository.findByStatusOrderByCreateTimeDesc(status, pageable);
        } else {
            articles = articleRepository.findAll(pageable);
        }
        
        List<ArticleDTO> dtoList = articles.getContent().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
            
        return new PageImpl<>(dtoList, pageable, articles.getTotalElements());
    }

    public ArticleDTO getArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        
        // Increment views
        article.setViews(article.getViews() + 1);
        articleRepository.save(article);
        
        return toDTO(article);
    }

    @Transactional
    public Long createArticle(CreateArticleRequest request) {
        Article article = new Article();
        article.setTitle(request.title());
        article.setSummary(request.summary());
        article.setContent(request.content());
        article.setCoverUrl(request.coverUrl());
        article.setStatus(request.status());
        article.setCategory(request.category());
        
        Article saved = articleRepository.save(article);
        log.info("Created article: {}", saved.getId());
        return saved.getId();
    }
    
    @Transactional
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
        log.info("Deleted article: {}", id);
    }

    private ArticleDTO toDTO(Article article) {
        return new ArticleDTO(
            article.getId(),
            article.getTitle(),
            article.getSummary(),
            article.getContent(),
            article.getCoverUrl(),
            article.getStatus(),
            article.getViews(),
            article.getCategory(),
            article.getCreateTime()
        );
    }
}
