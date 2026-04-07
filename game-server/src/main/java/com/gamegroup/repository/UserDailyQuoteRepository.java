package com.gamegroup.repository;

import com.gamegroup.entity.UserDailyQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserDailyQuoteRepository extends JpaRepository<UserDailyQuote, Long> {

    Optional<UserDailyQuote> findByUserIdAndQuoteDate(Long userId, LocalDate quoteDate);
}
