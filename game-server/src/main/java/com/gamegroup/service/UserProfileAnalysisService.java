package com.gamegroup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.dto.UserProfileAnalysisResult;
import com.gamegroup.dto.UserProfileDTO;
import java.io.InputStream;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileAnalysisService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LlmService llmService;
    private final MinioService minioService;

    public UserProfileAnalysisResult analyze(UserProfileDTO profile) {
        if (profile == null) {
            return fallbackResult(null, "资料为空，无法分析形象信息");
        }

        String faceImageDataUrl = buildImageDataUrl(profile.getFaceImageUrl());
        String frontBodyImageDataUrl = buildImageDataUrl(profile.getFrontBodyImageUrl());
        String sideBodyImageDataUrl = buildImageDataUrl(profile.getSideBodyImageUrl());
        if (isBlank(faceImageDataUrl) || isBlank(frontBodyImageDataUrl) || isBlank(sideBodyImageDataUrl)) {
            log.warn("形象分析缺少可用图片，回退基础摘要");
            return fallbackResult(profile, "资料图片读取失败，使用基础资料摘要");
        }

        String raw = llmService.chatWithImageForJson(
                buildSystemPrompt(),
                buildUserPrompt(profile) + "\n这是第1张资料图：面部照片，用于分析五官、脸型和气质。",
                faceImageDataUrl,
                llmService.getImageModel(),
                0.1D);

        String bodyRaw = llmService.chatWithImageForJson(
                buildSystemPrompt(),
                buildUserPrompt(profile) + "\n这是第2张资料图：正面全身照，用于分析身形比例、肩腰胯和整体轮廓。\n这是第3张资料图：侧面全身照已经在资料中提供，请在分析时综合考虑侧面体态、厚薄感和线条。",
                frontBodyImageDataUrl,
                llmService.getImageModel(),
                0.1D);

        String merged = mergeRawJson(raw, bodyRaw);
        try {
            JsonNode node = OBJECT_MAPPER.readTree(stripCodeFence(bodyRaw != null && !bodyRaw.isBlank() ? bodyRaw : raw));
            UserProfileAnalysisResult result = UserProfileAnalysisResult.builder()
                    .faceFeatures(text(node, "faceFeatures", "五官整体自然，气质清爽协调"))
                    .bodyFeatures(text(node, "bodyFeatures", "整体身形比例较协调，可通过版型进一步优化穿搭效果"))
                    .temperament(text(node, "temperament", defaultTemperament(profile)))
                    .styleDirection(text(node, "styleDirection", defaultStyleDirection(profile)))
                    .fitSuggestions(text(node, "fitSuggestions", "优先选择强调比例和线条感的版型"))
                    .colorSuggestions(text(node, "colorSuggestions", "优先选择和整体气质协调的中低饱和配色"))
                    .sceneSuggestions(text(node, "sceneSuggestions", "适合真实日常、通勤或轻松出行场景"))
                    .summaryText(text(node, "summaryText", buildSummary(profile)))
                    .rawJson(merged)
                    .build();
            log.info("用户形象分析完成: temperament={}, styleDirection={}", result.getTemperament(), result.getStyleDirection());
            return result;
        } catch (Exception e) {
            log.warn("形象分析 JSON 解析失败，回退基础摘要", e);
            return fallbackResult(profile, merged);
        }
    }

    private String buildSystemPrompt() {
        return "你是一名人物形象分析顾问。请严格输出单个 JSON 对象，不要输出 markdown，不要输出解释。"
                + "字段仅包含 faceFeatures, bodyFeatures, temperament, styleDirection, fitSuggestions, colorSuggestions, sceneSuggestions, summaryText。"
                + "分析重点是：五官和脸型、身形比例、气质、适合的版型方向、配色倾向和适用场景。"
                + "不要给出夸张、美颜式描述，要尽量真实、克制、可用于后续穿搭生成。";
    }

    private String buildUserPrompt(UserProfileDTO profile) {
        return "请根据用户基础资料和资料图，输出可用于后续穿搭参考图生成的人物画像摘要。"
                + "用户基础资料：性别=" + defaultText(profile.getGender(), "未指定")
                + "，身高=" + (profile.getHeightCm() == null ? "未提供" : profile.getHeightCm() + "cm")
                + "，体重=" + (profile.getWeightKg() == null ? "未提供" : profile.getWeightKg() + "kg")
                + "，日常风格=" + defaultText(profile.getDailyStyle(), "未提供")
                + "，备注=" + defaultText(profile.getBodyNote(), "未提供") + "。";
    }

    private UserProfileAnalysisResult fallbackResult(UserProfileDTO profile, String rawJson) {
        return UserProfileAnalysisResult.builder()
                .faceFeatures("五官和脸型信息以自然协调为主，适合真实日常人物表达")
                .bodyFeatures(profile == null ? "整体身形信息待补充" : buildBodyFeatures(profile))
                .temperament(defaultTemperament(profile))
                .styleDirection(defaultStyleDirection(profile))
                .fitSuggestions("优先突出比例、线条和整体协调感")
                .colorSuggestions("优先使用和日常风格一致的中低饱和配色")
                .sceneSuggestions("适合通勤、日常外出、轻松社交等真实生活场景")
                .summaryText(buildSummary(profile))
                .rawJson(rawJson)
                .build();
    }

    private String buildBodyFeatures(UserProfileDTO profile) {
        if (profile == null) {
            return "整体身形信息待补充";
        }
        StringBuilder builder = new StringBuilder();
        if (profile.getHeightCm() != null) {
            builder.append("身高约").append(profile.getHeightCm()).append("cm，");
        }
        if (profile.getWeightKg() != null) {
            builder.append("体重约").append(profile.getWeightKg()).append("kg，");
        }
        builder.append("整体身形比例以资料照为准，适合通过版型和层次强化协调感");
        return builder.toString();
    }

    private String defaultTemperament(UserProfileDTO profile) {
        return defaultText(profile == null ? null : profile.getDailyStyle(), "日常自然") + "，整体气质以真实生活感为主";
    }

    private String defaultStyleDirection(UserProfileDTO profile) {
        return defaultText(profile == null ? null : profile.getDailyStyle(), "简洁协调") + "，注重真实可落地的穿搭方向";
    }

    private String buildSummary(UserProfileDTO profile) {
        return defaultStyleDirection(profile) + "，结合脸型气质和身形比例，优先生成贴近本人、适合日常场景的穿搭参考图。";
    }

    private String mergeRawJson(String faceRaw, String bodyRaw) {
        StringBuilder builder = new StringBuilder();
        if (!isBlank(faceRaw)) {
            builder.append("face=").append(stripCodeFence(faceRaw));
        }
        if (!isBlank(bodyRaw)) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("body=").append(stripCodeFence(bodyRaw));
        }
        return builder.toString();
    }

    private String buildImageDataUrl(String imageUrl) {
        if (isBlank(imageUrl)) {
            return null;
        }
        try (InputStream inputStream = minioService.getStreamFromUrl(imageUrl)) {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                return null;
            }
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("读取用户资料图失败: {}", imageUrl, e);
            return null;
        }
    }

    private String stripCodeFence(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("```") || !trimmed.endsWith("```")) {
            return trimmed;
        }
        int firstLineBreak = trimmed.indexOf('\n');
        if (firstLineBreak < 0) {
            return trimmed.replace("```", "").trim();
        }
        return trimmed.substring(firstLineBreak + 1, trimmed.length() - 3).trim();
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String result = value.asText();
        return result == null || result.isBlank() ? defaultValue : result.trim();
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
