package com.gamegroup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.config.LlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通用 LLM 调用服务（OpenAI 兼容格式）
 * 支持 OpenAI、DeepSeek、通义千问等兼容 OpenAI API 格式的服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmConfig llmConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 LLM 生成文本
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 生成的文本内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, 0.8D);
    }

    public String chat(String systemPrompt, String userPrompt, double temperature) {
        if (!llmConfig.isEnabled()) {
            log.warn("LLM 功能未启用");
            return null;
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", resolveModel(llmConfig, null),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", temperature,
                    "max_tokens", 2000
            );

            return executeChatCompletion(requestBody, llmConfig.getBaseUrl(), llmConfig.getApiKey());

        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            return null;
        }
    }

    public String chatWithImageForJson(String systemPrompt, String userPrompt, String imageDataUrl, String model, double temperature) {
        LlmConfig.VisionConfig visionConfig = llmConfig.getVision();
        if (!isVisionEnabled(visionConfig)) {
            log.warn("图片整理功能未启用");
            return null;
        }

        try {
            String resolvedModel = model != null && !model.isBlank() ? model.trim() : resolveVisionModel(visionConfig);
            String resolvedBaseUrl = resolveBaseUrl(visionConfig);
            String resolvedApiKey = resolveApiKey(visionConfig);
            String mergedPrompt = systemPrompt == null || systemPrompt.isBlank()
                    ? userPrompt
                    : systemPrompt + "\n" + userPrompt;

            log.info("开始图片整理请求: baseUrl={}, model={}, imageSource={}, promptLength={}",
                    normalizeBaseUrl(resolvedBaseUrl),
                    resolvedModel,
                    describeImageSource(imageDataUrl),
                    mergedPrompt == null ? 0 : mergedPrompt.length());

            Map<String, Object> requestBody = buildImageRequestBody(resolvedModel, mergedPrompt, imageDataUrl, temperature, true);

            String response = executeChatCompletion(requestBody, resolvedBaseUrl, resolvedApiKey);
            if (response != null && !response.isBlank()) {
                log.info("图片整理请求返回成功: model={}, responsePreview={}", resolvedModel, previewText(response));
                return response;
            }

            log.warn("带 JSON 约束的图片整理返回为空，回退为普通图片对话");
            String fallbackResponse = executeChatCompletion(
                    buildImageRequestBody(resolvedModel, mergedPrompt, imageDataUrl, temperature, false),
                    resolvedBaseUrl,
                    resolvedApiKey);
            if (fallbackResponse != null && !fallbackResponse.isBlank()) {
                log.info("图片整理普通模式返回成功: model={}, responsePreview={}", resolvedModel, previewText(fallbackResponse));
            }
            return fallbackResponse;

        } catch (Exception e) {
            log.error("图片整理调用失败", e);
            return null;
        }
    }

    private String executeChatCompletion(Map<String, Object> requestBody, String baseUrl, String apiKey) throws Exception {
        String url = normalizeBaseUrl(baseUrl) + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content == null || content.isNull()) {
                        return null;
                    }
                    if (content.isTextual()) {
                        return content.asText();
                    }
                    if (content.isArray()) {
                        return content.findValuesAsText("text").stream()
                                .filter(Objects::nonNull)
                                .filter(value -> !value.isBlank())
                                .findFirst()
                                .orElse(null);
                    }
                }
            }
        }

        log.error("LLM 调用返回异常: {}", response.getBody());
        return null;
    }

    public String getImageModel() {
        return resolveVisionModel(llmConfig.getVision());
    }

    public String getVisionConfigSummary() {
        LlmConfig.VisionConfig visionConfig = llmConfig.getVision();
        return String.format("enabled=%s, baseUrl=%s, model=%s",
                isVisionEnabled(visionConfig),
                normalizeBaseUrl(resolveBaseUrl(visionConfig)),
                resolveVisionModel(visionConfig));
    }

    private Map<String, Object> buildImageRequestBody(String model, String prompt, String imageDataUrl, double temperature, boolean jsonMode) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl)),
                        Map.of("type", "text", "text", prompt)
                ))
        ));
        body.put("temperature", temperature);
        body.put("max_tokens", 2000);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    private String resolveModel(LlmConfig config, String preferredModel) {
        if (preferredModel != null && !preferredModel.isBlank()) {
            return preferredModel.trim();
        }
        return config.getModel();
    }

    private String resolveVisionModel(LlmConfig.VisionConfig config) {
        if (config != null && config.getModel() != null && !config.getModel().isBlank()) {
            return config.getModel().trim();
        }
        if (llmConfig.getImageModel() != null && !llmConfig.getImageModel().isBlank()) {
            return llmConfig.getImageModel().trim();
        }
        return llmConfig.getModel();
    }

    private boolean isVisionEnabled(LlmConfig.VisionConfig config) {
        return config != null && config.isEnabled();
    }

    private String resolveBaseUrl(LlmConfig.VisionConfig config) {
        if (config != null && config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            return config.getBaseUrl().trim();
        }
        return llmConfig.getBaseUrl();
    }

    private String resolveApiKey(LlmConfig.VisionConfig config) {
        if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) {
            return config.getApiKey().trim();
        }
        return llmConfig.getApiKey();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return llmConfig.getBaseUrl();
        }
        String normalized = baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String describeImageSource(String imageDataUrl) {
        if (imageDataUrl == null || imageDataUrl.isBlank()) {
            return "empty";
        }
        if (imageDataUrl.startsWith("data:")) {
            return "data-url(" + imageDataUrl.length() + ")";
        }
        return "url";
    }

    private String previewText(String value) {
        if (value == null) {
            return "null";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180) + "...";
    }

    /**
     * 简单调用（无系统提示词）
     */
    public String chat(String userPrompt) {
        return chat("You are a helpful assistant.", userPrompt, 0.8D);
    }
}
