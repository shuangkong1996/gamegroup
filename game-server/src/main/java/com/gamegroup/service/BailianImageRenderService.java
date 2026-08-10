package com.gamegroup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.config.LlmConfig;
import com.gamegroup.dto.OutfitRenderTaskItemDTO;
import com.gamegroup.dto.UserProfileAnalysisResult;
import com.gamegroup.dto.UserProfileDTO;
import com.gamegroup.entity.OutfitRenderTask;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BailianImageRenderService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LlmConfig llmConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public String submitRenderTask(OutfitRenderTask task, UserProfileDTO profile, UserProfileAnalysisResult analysis, List<OutfitRenderTaskItemDTO> items) {
        LlmConfig.RenderConfig config = llmConfig.getRender();
        if (config == null || !config.isEnabled() || isBlank(config.getApiKey())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "百炼生图未启用或未配置 API Key");
        }

        try {
            SubmitPayload payload = buildSubmitPayload(task, profile, analysis, items, config);
            log.info("提交百炼出图任务: taskId={}, mode={}, model={}, apiPath={}", task.getId(), task.getMode(), String.valueOf(payload.body().get("model")), payload.path());

            HttpHeaders headers = buildHeaders(config.getApiKey());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload.body(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizeBaseUrl(config.getBaseUrl()) + payload.path(),
                    HttpMethod.POST,
                    entity,
                    String.class);

            JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
            String imageUrl = firstNonBlank(
                    textAt(root, "/output/choices/0/message/content/0/image"),
                    textAt(root, "/output/image_url"),
                    textAt(root, "/output/results/0/url"));
            if (isBlank(imageUrl)) {
                log.error("百炼图像编辑返回异常，未返回 image url: {}", response.getBody());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "百炼生图任务提交失败");
            }
            log.info("百炼图像编辑同步返回成功: taskId={}, imageUrl={}", task.getId(), imageUrl);
            return imageUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("百炼生图任务提交失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "百炼生图任务提交失败");
        }
    }

    public RenderStatus pollUntilFinished(String providerTaskId) {
        if (providerTaskId != null && (providerTaskId.startsWith("http://") || providerTaskId.startsWith("https://"))) {
            return new RenderStatus("SUCCESS", providerTaskId, null);
        }
        LlmConfig.RenderConfig config = llmConfig.getRender();
        Instant deadline = Instant.now().plus(Duration.ofSeconds(Math.max(15, config.getPollTimeoutSeconds())));
        RenderStatus latest = queryTask(providerTaskId);
        while (latest.isRunning() && Instant.now().isBefore(deadline)) {
            sleep(Math.max(1000, config.getPollIntervalMillis()));
            latest = queryTask(providerTaskId);
        }
        return latest;
    }

    public RenderStatus queryTask(String providerTaskId) {
        LlmConfig.RenderConfig config = llmConfig.getRender();
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(config.getApiKey()));
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizeBaseUrl(config.getBaseUrl()) + "/tasks/" + providerTaskId,
                    HttpMethod.GET,
                    entity,
                    String.class);

            JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
            String taskStatus = textAt(root, "/output/task_status");
            String imageUrl = textAt(root, "/output/image_url");
            if (imageUrl == null) {
                imageUrl = textAt(root, "/output/results/0/url");
            }
            String message = textAt(root, "/output/message");
            log.info("查询百炼出图状态: providerTaskId={}, status={}, hasImageUrl={}", providerTaskId, taskStatus, imageUrl != null && !imageUrl.isBlank());

            if (equalsAnyIgnoreCase(taskStatus, "SUCCEEDED", "SUCCESS")) {
                return new RenderStatus("SUCCESS", imageUrl, null);
            }
            if (equalsAnyIgnoreCase(taskStatus, "FAILED", "CANCELED")) {
                return new RenderStatus("FAILED", null, isBlank(message) ? "百炼生图失败" : message);
            }
            return new RenderStatus("RUNNING", imageUrl, null);
        } catch (Exception e) {
            log.error("百炼生图任务查询失败: taskId={}", providerTaskId, e);
            return new RenderStatus("FAILED", null, "百炼生图任务查询失败");
        }
    }

    private Map<String, Object> buildParameters(LlmConfig.RenderConfig config) {
        java.util.LinkedHashMap<String, Object> parameters = new java.util.LinkedHashMap<>();
        parameters.put("size", config.getSize());
        parameters.put("n", 1);
        if (!isBlank(config.getStyle())) {
            parameters.put("style", config.getStyle().trim());
        }
        if (!isBlank(config.getNegativePrompt())) {
            parameters.put("negative_prompt", config.getNegativePrompt().trim());
        }
        return parameters;
    }

    private SubmitPayload buildSubmitPayload(
            OutfitRenderTask task,
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            List<OutfitRenderTaskItemDTO> items,
            LlmConfig.RenderConfig config) {
        String prompt = buildPrompt(task, profile, analysis, items);
        java.util.LinkedHashMap<String, Object> input = new java.util.LinkedHashMap<>();
        List<Map<String, Object>> content = buildImageContent(profile, items);
        content.add(Map.of("text", prompt));
        input.put("messages", List.of(Map.of("role", "user", "content", content)));

        Map<String, Object> textBody = Map.of(
                "model", resolveTextModel(config),
                "input", input,
                "parameters", buildEditParameters(config));
        return new SubmitPayload("/services/aigc/multimodal-generation/generation", textBody);
    }

    private List<Map<String, Object>> buildImageContent(UserProfileDTO profile, List<OutfitRenderTaskItemDTO> items) {
        List<Map<String, Object>> content = new ArrayList<>();
        if (profile == null) {
            addReferenceItemImages(content, items, 3);
            return content;
        }

        boolean hasItemImages = hasReferenceItemImages(items);
        addImageContent(content, profile.getFaceImageUrl(), 3);
        if (hasItemImages) {
            addReferenceItemImages(content, items, 3);
            addImageContent(content, firstNonBlank(profile.getFrontBodyImageUrl(), profile.getSideBodyImageUrl()), 3);
        } else {
            addImageContent(content, profile.getFrontBodyImageUrl(), 3);
            addImageContent(content, profile.getSideBodyImageUrl(), 3);
        }
        return content;
    }

    private Map<String, Object> buildEditParameters(LlmConfig.RenderConfig config) {
        java.util.LinkedHashMap<String, Object> parameters = new java.util.LinkedHashMap<>();
        parameters.put("n", 1);
        parameters.put("watermark", false);
        parameters.put("prompt_extend", true);
        parameters.put("size", config.getSize());
        if (!isBlank(config.getNegativePrompt())) {
            parameters.put("negative_prompt", config.getNegativePrompt().trim());
        }
        return parameters;
    }

    private void addImageContent(List<Map<String, Object>> content, String imageUrl, int maxImages) {
        if (content.size() >= maxImages) {
            return;
        }
        if (!isBlank(imageUrl) && content.stream().noneMatch(item -> imageUrl.trim().equals(item.get("image")))) {
            content.add(Map.of("image", imageUrl.trim()));
        }
    }

    private void addReferenceItemImages(List<Map<String, Object>> content, List<OutfitRenderTaskItemDTO> items, int maxImages) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OutfitRenderTaskItemDTO item : items) {
            if (content.size() >= maxImages) {
                break;
            }
            if (item == null || isBlank(item.getImageUrl())) {
                continue;
            }
            addImageContent(content, item.getImageUrl(), maxImages);
        }
    }

    private boolean hasReferenceItemImages(List<OutfitRenderTaskItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream().anyMatch(item -> item != null && !isBlank(item.getImageUrl()));
    }

    private String resolveTextModel(LlmConfig.RenderConfig config) {
        if (!isBlank(config.getTextModel())) {
            return config.getTextModel().trim();
        }
        return config.getModel();
    }

    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private String buildPrompt(OutfitRenderTask task, UserProfileDTO profile, UserProfileAnalysisResult analysis, List<OutfitRenderTaskItemDTO> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(task.getRenderPrompt() == null ? "" : task.getRenderPrompt().trim());
        builder.append("。人物要求：请强参考输入中的面部照片来还原用户本人的五官、脸型和气质，面部相似度优先级最高。");
        builder.append(" 请同时参考正面全身照和侧面全身照来还原身形比例、肩腰胯、站姿和整体轮廓。最终必须重新生成一张新的全身穿搭参考图，不要把任意输入照片直接当作底图简单修改。");
        if (profile != null) {
            builder.append(" 用户基础信息：")
                    .append(nullToDefault(profile.getGender(), "未指定性别")).append("，")
                    .append(profile.getHeightCm() == null ? "身高未提供" : ("身高" + profile.getHeightCm() + "cm")).append("，")
                    .append(profile.getWeightKg() == null ? "体重未提供" : ("体重" + profile.getWeightKg() + "kg")).append("，")
                    .append(nullToDefault(profile.getDailyStyle(), "日常风格未提供")).append("。");
            if (!isBlank(profile.getBodyNote())) {
                builder.append(" 补充偏好：").append(profile.getBodyNote().trim()).append("。");
            }
            builder.append(" 输入图说明：若包含面部照片，请优先强参考脸部；若包含正面或侧面全身照，请参考体态比例、轮廓和站姿；若包含衣物图片，请仅参考版型、颜色、材质与风格，不要直接贴图。");
        }
        if (analysis != null) {
            builder.append(" 人物画像摘要：")
                    .append(nullToDefault(analysis.getSummaryText(), "以真实自然的人物气质和身形比例为主")).append("。")
                    .append(" 五官参考：").append(nullToDefault(analysis.getFaceFeatures(), "自然协调")).append("。")
                    .append(" 身形参考：").append(nullToDefault(analysis.getBodyFeatures(), "比例协调")).append("。")
                    .append(" 气质方向：").append(nullToDefault(analysis.getTemperament(), "真实日常")).append("。");
        }
        if (items != null && !items.isEmpty()) {
            builder.append(" 衣物参考：额外输入的衣物图片只作为版型、颜色、材质和风格参考，不要求逐像素复刻，但要尽量保持相近穿搭感觉。不要直接把衣物图抠图贴到人物身上。 ");
            for (OutfitRenderTaskItemDTO item : items) {
                builder.append("[")
                        .append(nullToDefault(item.getItemRole(), "ITEM"))
                        .append(":")
                        .append(nullToDefault(item.getName(), "建议单品"));
                if (!isBlank(item.getCategoryLevel1())) {
                    builder.append(",分类=").append(item.getCategoryLevel1().trim());
                }
                if (!isBlank(item.getColor())) {
                    builder.append(",颜色=").append(item.getColor().trim());
                }
                if (!isBlank(item.getSummaryText())) {
                    builder.append(",说明=").append(item.getSummaryText().trim());
                }
                builder.append("] ");
            }
        }
        builder.append(" 背景要符合场景：")
                .append(nullToDefault(task.getSceneRequirement(), "日常出门"))
                .append("。天气：")
                .append(nullToDefault(task.getWeatherCondition(), "未知天气"))
                .append("，")
                .append(task.getTemperatureMin() == null ? "" : task.getTemperatureMin() + "~")
                .append(task.getTemperatureMax() == null ? "未知温度" : task.getTemperatureMax() + "度")
                .append("。画面要求：高质量、清晰、人体结构正常、服装细节自然、人物面部不能扭曲、五官必须自然协调、最终人物要明显贴近用户本人，整体像写真级参考图。");
        return builder.toString();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String resolved = baseUrl == null || baseUrl.isBlank() ? "https://dashscope.aliyuncs.com/api/v1" : baseUrl.trim();
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private String textAt(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean equalsAnyIgnoreCase(String value, String... candidates) {
        if (value == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String nullToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public record RenderStatus(String status, String imageUrl, String errorMessage) {
        public boolean isRunning() {
            return "RUNNING".equals(status);
        }
    }

    private record SubmitPayload(String path, Map<String, Object> body) {
    }
}
