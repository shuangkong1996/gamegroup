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
     * 穿搭参考图专用生图配置
     */
    private RenderConfig render = new RenderConfig();

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

    @Data
    public static class RenderConfig {

        /**
         * 是否启用百炼生图
         */
        private boolean enabled = false;

        /**
         * 百炼 API Key
         */
        private String apiKey;

        /**
         * 百炼 API Base URL
         */
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";

        /**
         * 通用生图模型名称（兼容旧配置）
         */
        private String model = "wanx2.1-t2i-turbo";

        /**
         * 图像编辑/融合模型
         */
        private String textModel = "qwen-image-edit-max-2026-01-16";

        /**
         * 试衣模式使用的模型
         */
        private String tryonModel = "aitryon";

        /**
         * 输出尺寸，例如 1024*1024
         */
        private String size = "1024*1024";

        /**
         * 风格控制，可空
         */
        private String style;

        /**
         * 负面提示词，可空
         */
        private String negativePrompt;

        /**
         * 单次轮询等待秒数
         */
        private int pollTimeoutSeconds = 90;

        /**
         * 轮询间隔毫秒
         */
        private int pollIntervalMillis = 3000;
    }
}
