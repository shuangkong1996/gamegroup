package com.gamegroup.repository;

import com.gamegroup.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByStatusTrue();

    @Query(value = "SELECT * FROM t_quote WHERE status = true ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Quote findRandomQuote();
}
