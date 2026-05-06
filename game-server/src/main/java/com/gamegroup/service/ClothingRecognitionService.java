package com.gamegroup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.dto.ClothingRecognitionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothingRecognitionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LlmService llmService;
    private final MinioService minioService;

    public ClothingRecognitionResult recognizeFromImage(String imageUrl, String imageFileName) {
        log.info("开始整理衣物图片: imageFileName={}, imageUrl={}", imageFileName, imageUrl);
        log.info("当前图片整理配置: {}", llmService.getVisionConfigSummary());

        String imageDataUrl = buildImageDataUrl(imageUrl, imageFileName);
        if (imageDataUrl == null || imageDataUrl.isBlank()) {
            log.warn("衣物图片读取失败，进入待补充流程: imageFileName={}", imageFileName);
            return fallbackResult(imageUrl, null, "当前信息待补充，可手动完善");
        }

        String raw = llmService.chatWithImageForJson(
                buildSystemPrompt(),
                buildUserPrompt(),
                imageDataUrl,
                llmService.getImageModel(),
                0.1D);
        if (raw == null || raw.isBlank()) {
            log.warn("衣物整理返回为空，使用兜底结果: imageUrl={}", imageUrl);
            return fallbackResult(imageUrl, null, "当前信息待补充，可手动完善");
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(stripCodeFence(raw));
            ClothingRecognitionResult result = ClothingRecognitionResult.builder()
                    .fallback(false)
                    .fallbackReason(null)
                    .name(text(node, "name", "未命名衣物"))
                    .categoryLevel1(text(node, "categoryLevel1", "其他"))
                    .categoryLevel2(text(node, "categoryLevel2", "未分类"))
                    .color(text(node, "color", "未知"))
                    .patternTags(text(node, "patternTags", ""))
                    .seasonTags(text(node, "seasonTags", "四季"))
                    .styleTags(text(node, "styleTags", "日常"))
                    .occasionTags(text(node, "occasionTags", "日常"))
                    .material(text(node, "material", "未知"))
                    .thicknessLevel(levelValue(node, "thicknessLevel", 2))
                    .warmthLevel(levelValue(node, "warmthLevel", 2))
                    .waterproofLevel(levelValue(node, "waterproofLevel", 0))
                    .fitType(text(node, "fitType", "常规"))
                    .summaryText(text(node, "summaryText", "已整理基础衣物信息"))
                    .matchPoints(text(node, "matchPoints", "适合日常基础搭配"))
                    .avoidPoints(text(node, "avoidPoints", "暂无特别避让信息"))
                    .confidenceScore(scoreValue(node, "confidenceScore", 0.70D))
                    .rawJson(stripCodeFence(raw))
                    .build();
            log.info("衣物整理成功: categoryLevel1={}, categoryLevel2={}, color={}, confidenceScore={}",
                    result.getCategoryLevel1(),
                    result.getCategoryLevel2(),
                    result.getColor(),
                    result.getConfidenceScore());
            return result;
        } catch (Exception e) {
            log.error("衣物整理 JSON 解析失败，使用兜底结果: imageUrl={}", imageUrl, e);
            return fallbackResult(imageUrl, raw, "当前信息待补充，可手动完善");
        }
    }

    private String buildSystemPrompt() {
        return "你是服饰整理助手。你需要根据输入图片整理衣物信息。"
                + "严格返回单个 JSON 对象，不要输出 markdown，不要输出解释文字，不要补充对象外内容。"
                + "字段仅包含 name, categoryLevel1, categoryLevel2, color, patternTags, seasonTags, styleTags, occasionTags, material, thicknessLevel, warmthLevel, waterproofLevel, fitType, summaryText, matchPoints, avoidPoints, confidenceScore。"
                + "categoryLevel1 仅可取 上装、下装、外套、裙装、鞋子、包袋、配饰、其他 之一。"
                + "thicknessLevel、warmthLevel、waterproofLevel 必须是 0 到 3 的整数。"
                + "confidenceScore 必须是 0 到 1 的小数。"
                + "如果无法确认某个字段，请给出保守值，不要编造明显细节。";
    }

    private String buildUserPrompt() {
        return "请只根据这张衣物图片整理信息。"
                + "优先保证一级分类、二级分类、颜色、厚薄、保暖程度准确。"
                + "难以确认的材质、场景、风格请保守填写。";
    }

    private ClothingRecognitionResult fallbackResult(String imageUrl, String rawJson, String reason) {
        return ClothingRecognitionResult.builder()
                .fallback(true)
                .fallbackReason(reason)
                .name("待确认衣物")
                .categoryLevel1("其他")
                .categoryLevel2("未分类")
                .color("未知")
                .patternTags("")
                .seasonTags("四季")
                .styleTags("日常")
                .occasionTags("日常")
                .material("未知")
                .thicknessLevel(2)
                .warmthLevel(2)
                .waterproofLevel(0)
                .fitType("常规")
                .summaryText("已完成基础整理，请手动补充更准确的信息")
                .matchPoints("适合日常基础搭配")
                .avoidPoints("暂无")
                .confidenceScore(0.50D)
                .rawJson(rawJson == null ? "{\"imageUrl\":\"" + imageUrl + "\"}" : rawJson)
                .build();
    }

    private String buildImageDataUrl(String imageUrl, String imageFileName) {
        try (InputStream inputStream = imageFileName != null && !imageFileName.isBlank()
                ? minioService.getFileStream(imageFileName)
                : minioService.getStreamFromUrl(imageUrl)) {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                return null;
            }
            String mimeType = resolveMimeType(imageFileName, imageUrl);
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("读取衣物图片失败: imageUrl={}, imageFileName={}", imageUrl, imageFileName, e);
            return null;
        }
    }

    private String resolveMimeType(String imageFileName, String imageUrl) {
        String source = imageFileName;
        if (source == null || source.isBlank()) {
            source = imageUrl;
        }
        if (source == null) {
            return "image/jpeg";
        }

        String lowerSource = source.toLowerCase();
        if (lowerSource.endsWith(".png")) {
            return "image/png";
        }
        if (lowerSource.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerSource.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
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

    private Integer levelValue(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        int level = value == null || value.isNull() ? defaultValue : value.asInt(defaultValue);
        return Math.max(0, Math.min(3, level));
    }

    private Double scoreValue(JsonNode node, String field, double defaultValue) {
        JsonNode value = node.get(field);
        double score = value == null || value.isNull() ? defaultValue : value.asDouble(defaultValue);
        return Math.max(0D, Math.min(1D, score));
    }
}
