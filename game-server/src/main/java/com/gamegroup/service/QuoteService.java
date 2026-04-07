package com.gamegroup.service;

import com.gamegroup.dto.QuoteDTO;
import com.gamegroup.entity.Quote;
import com.gamegroup.entity.UserDailyQuote;
import com.gamegroup.repository.QuoteRepository;
import com.gamegroup.repository.UserDailyQuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final UserDailyQuoteRepository userDailyQuoteRepository;

    /**
     * 获取用户今日专属语录
     * 如果今日已有分配的语录则返回，否则随机分配一条
     */
    @Transactional
    public QuoteDTO getTodayQuote(Long userId) {
        LocalDate today = LocalDate.now();

        // 查找今日是否已有分配的语录
        UserDailyQuote userDailyQuote = userDailyQuoteRepository
                .findByUserIdAndQuoteDate(userId, today)
                .orElse(null);

        Quote quote;

        if (userDailyQuote != null) {
            // 已有今日语录，直接返回
            quote = quoteRepository.findById(userDailyQuote.getQuoteId()).orElse(null);
        } else {
            // 随机分配一条语录
            quote = quoteRepository.findRandomQuote();

            if (quote != null) {
                // 保存用户今日语录关联
                UserDailyQuote newRecord = new UserDailyQuote();
                newRecord.setUserId(userId);
                newRecord.setQuoteId(quote.getId());
                newRecord.setQuoteDate(today);
                userDailyQuoteRepository.save(newRecord);
            }
        }

        return toDTO(quote);
    }

    /**
     * 获取随机语录（无需登录）
     */
    public QuoteDTO getRandomQuote() {
        Quote quote = quoteRepository.findRandomQuote();
        return toDTO(quote);
    }

    private QuoteDTO toDTO(Quote quote) {
        if (quote == null) {
            // 如果没有语录数据，返回默认
            QuoteDTO dto = new QuoteDTO();
            dto.setContent("打工人，打工魂，打工都是人上人！");
            dto.setEmoji("💪");
            return dto;
        }

        QuoteDTO dto = new QuoteDTO();
        dto.setId(quote.getId());
        dto.setContent(quote.getContent());
        dto.setEmoji(quote.getEmoji());
        return dto;
    }
}
