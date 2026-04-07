package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.QuoteDTO;
import com.gamegroup.service.QuoteGeneratorService;
import com.gamegroup.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "语录接口")
@RestController
@RequestMapping("/quote")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;
    private final QuoteGeneratorService quoteGeneratorService;

    @Operation(summary = "获取今日专属语录")
    @GetMapping("/today")
    public ApiResponse<QuoteDTO> getTodayQuote(@RequestAttribute("userId") Long userId) {
        QuoteDTO quote = quoteService.getTodayQuote(userId);
        return ApiResponse.success(quote);
    }

    @Operation(summary = "获取随机语录（无需登录）")
    @GetMapping("/random")
    public ApiResponse<QuoteDTO> getRandomQuote() {
        QuoteDTO quote = quoteService.getRandomQuote();
        return ApiResponse.success(quote);
    }

    @Operation(summary = "手动触发生成语录（测试用）")
    @PostMapping("/generate")
    public ApiResponse<Integer> generateQuotes(@RequestParam(defaultValue = "5") int count) {
        int generated = quoteGeneratorService.generateDailyQuotes(count);
        return ApiResponse.success(generated);
    }
}
