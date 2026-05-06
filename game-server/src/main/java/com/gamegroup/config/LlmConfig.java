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
    private String model = "deepseek-v4-flash";

    /**
     * 图片整理使用的模型名称，为空时回退到 model
     */
    private String imageModel;

    /**
     * 图片整理专用配置
     */
    private VisionConfig vision = new VisionConfig();

    /**
     * 是否启用 LLM 功能
     */
    private boolean enabled = true;

    @Data
    public static class VisionConfig {

        /**
         * 是否启用图片整理专用配置
         */
        private boolean enabled = false;

        /**
         * 图片整理专用 API Key
         */
        private String apiKey;

        /**
         * 图片整理专用 API Base URL
         */
        private String baseUrl;

        /**
         * 图片整理专用模型名称
         */
        private String model;
    }
}
