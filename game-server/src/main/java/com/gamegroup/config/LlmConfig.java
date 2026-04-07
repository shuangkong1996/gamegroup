package com.gamegroup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API Base URL (OpenAI compatible)
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * Model name
     */
    private String model = "deepseek-chat";

    /**
     * 是否启用 LLM 功能
     */
    private boolean enabled = true;
}
