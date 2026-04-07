package com.gamegroup.scheduler;

import com.gamegroup.service.QuoteGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 语录定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteScheduler {

    private final QuoteGeneratorService quoteGeneratorService;

    /**
     * 每天凌晨 2:00 执行，生成当日语录
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyQuotes() {
        log.info("===== 开始执行每日语录生成任务 =====");

        try {
            // 每天生成 5 条语录
            int count = quoteGeneratorService.generateDailyQuotes(5);
            log.info("===== 每日语录生成任务完成，生成 {} 条 =====", count);
        } catch (Exception e) {
            log.error("每日语录生成任务执行失败", e);
        }
    }
}
