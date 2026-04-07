package com.gamegroup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.entity.Quote;
import com.gamegroup.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 语录生成服务 - 通过 LLM 生成每日语录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteGeneratorService {

    private final LlmService llmService;
    private final QuoteRepository quoteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        你是一个专业的文案创作者，擅长创作打工人/职场人喜欢的语录。

        你的任务是根据最近的热点事件、热门影视剧、综艺节目、网络流行语等，创作出能引起共鸣的语录。

        语录风格可以是：
        - 正能量激励（如"人没有梦想，跟咸鱼有什么区别？"）
        - 哲学思考（如"终一生渡世人，终一世渡一人，其实是一样的"）
        - 俏皮幽默（如"我不是在摸鱼，我是在做战略性休息"）
        - 直击人性（如"成年人的崩溃往往就在一瞬间"）
        - 温暖治愈（如"你已经很努力了，要对自己好一点"）

        要求：
        1. 每条语录简短有力，不超过50个字
        2. 配一个合适的 emoji 表情
        3. 内容积极向上，适合在工作场合分享
        4. 返回 JSON 数组格式
        """;

    /**
     * 生成每日语录
     *
     * @param count 生成数量
     * @return 生成的语录数量
     */
    @Transactional
    public int generateDailyQuotes(int count) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        String userPrompt = String.format("""
            今天是 %s，请根据最近的热点事件、热门影视剧、综艺节目（如喜人奇妙夜、脱口秀大会等）、
            经典电影台词（如周星驰电影）、网络流行语等，创作 %d 条能引起打工人共鸣的语录。

            请以 JSON 数组格式返回，每个元素包含 content（语录内容）和 emoji（表情）字段。

            示例格式：
            [
              {"content": "人没有梦想，跟咸鱼有什么区别？", "emoji": "🐟"},
              {"content": "终一生渡世人，终一世渡一人", "emoji": "🌟"}
            ]

            只返回 JSON 数组，不要其他内容。
            """, today, count);

        try {
            String response = llmService.chat(SYSTEM_PROMPT, userPrompt);
            if (response == null || response.isEmpty()) {
                log.error("LLM 返回为空");
                return 0;
            }

            // 提取 JSON 内容（处理可能的 markdown 代码块）
            String jsonContent = extractJsonArray(response);

            List<Map<String, String>> quotes = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Map<String, String>>>() {}
            );

            int savedCount = 0;
            for (Map<String, String> quoteMap : quotes) {
                String content = quoteMap.get("content");
                String emoji = quoteMap.get("emoji");

                if (content != null && !content.isEmpty()) {
                    Quote quote = new Quote();
                    quote.setContent(content);
                    quote.setEmoji(emoji != null ? emoji : "💬");
                    quote.setStatus(true);
                    quoteRepository.save(quote);
                    savedCount++;
                    log.info("保存语录: {} {}", emoji, content);
                }
            }

            log.info("成功生成 {} 条语录", savedCount);
            return savedCount;

        } catch (Exception e) {
            log.error("生成语录失败", e);
            return 0;
        }
    }

    /**
     * 从响应中提取 JSON 数组
     */
    private String extractJsonArray(String response) {
        String content = response.trim();

        // 处理 markdown 代码块
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.indexOf("```", start);
            if (end > start) {
                content = content.substring(start, end).trim();
            }
        } else if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.indexOf("```", start);
            if (end > start) {
                content = content.substring(start, end).trim();
            }
        }

        // 找到 JSON 数组的开始和结束
        int arrayStart = content.indexOf('[');
        int arrayEnd = content.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            content = content.substring(arrayStart, arrayEnd + 1);
        }

        return content;
    }
}
