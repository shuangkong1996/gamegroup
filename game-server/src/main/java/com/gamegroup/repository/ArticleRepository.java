package com.gamegroup.repository;

import com.gamegroup.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByStatusOrderByCreateTimeDesc(Integer status, Pageable pageable);
    
    List<Article> findByStatus(Integer status);
}
