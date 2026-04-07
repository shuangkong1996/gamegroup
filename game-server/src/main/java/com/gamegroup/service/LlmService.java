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
        if (!llmConfig.isEnabled()) {
            log.warn("LLM 功能未启用");
            return null;
        }

        try {
            String url = llmConfig.getBaseUrl() + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmConfig.getApiKey());

            Map<String, Object> requestBody = Map.of(
                    "model", llmConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.8,
                    "max_tokens", 2000
            );

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
                        return message.get("content").asText();
                    }
                }
            }

            log.error("LLM 调用返回异常: {}", response.getBody());
            return null;

        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            return null;
        }
    }

    /**
     * 简单调用（无系统提示词）
     */
    public String chat(String userPrompt) {
        return chat("You are a helpful assistant.", userPrompt);
    }
}
