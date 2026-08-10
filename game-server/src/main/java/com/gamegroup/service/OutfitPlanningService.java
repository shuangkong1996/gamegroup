package com.gamegroup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.dto.OutfitRenderPlanResult;
import com.gamegroup.dto.OutfitRenderTaskItemDTO;
import com.gamegroup.dto.UserProfileAnalysisResult;
import com.gamegroup.dto.UserProfileDTO;
import com.gamegroup.dto.WeatherSnapshotDTO;
import com.gamegroup.entity.WardrobeItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitPlanningService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LlmService llmService;

    public OutfitRenderPlanResult planWithWardrobe(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement,
            List<WardrobeItem> wardrobeItems) {
        log.info("开始生成衣柜模式参考方案: scene={}, weather={}, itemCount={}", normalizeScene(sceneRequirement), weatherSummary(weather), wardrobeItems == null ? 0 : wardrobeItems.size());
        OutfitRenderPlanResult llmResult = tryPlanWithWardrobeByLlm(profile, analysis, weather, sceneRequirement, wardrobeItems);
        if (llmResult != null && llmResult.getItems() != null && !llmResult.getItems().isEmpty()) {
            log.info("衣柜模式参考方案生成完成: source=llm, selectedItemCount={}", llmResult.getItems().size());
            return llmResult;
        }
        log.info("衣柜模式参考方案回退规则生成");
        return fallbackWardrobePlan(profile, analysis, weather, sceneRequirement, wardrobeItems);
    }

    public OutfitRenderPlanResult planFreeGenerate(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement) {
        log.info("开始生成自由模式参考方案: scene={}, weather={}", normalizeScene(sceneRequirement), weatherSummary(weather));
        OutfitRenderPlanResult llmResult = tryPlanFreeGenerateByLlm(profile, analysis, weather, sceneRequirement);
        if (llmResult != null) {
            log.info("自由模式参考方案生成完成: source=llm, conceptItemCount={}", llmResult.getItems() == null ? 0 : llmResult.getItems().size());
            return llmResult;
        }
        log.info("自由模式参考方案回退规则生成");
        return fallbackFreePlan(profile, analysis, weather, sceneRequirement);
    }

    private OutfitRenderPlanResult tryPlanWithWardrobeByLlm(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement,
            List<WardrobeItem> wardrobeItems) {
        try {
            String systemPrompt = "你是一名专业穿搭顾问。请严格输出 JSON，不要输出 markdown，不要输出解释。";
            String userPrompt = buildWardrobePlanningPrompt(profile, analysis, weather, sceneRequirement, wardrobeItems);
            String response = llmService.chat(systemPrompt, userPrompt, 0.4D);
            return parseWardrobePlanResponse(response, wardrobeItems, analysis, weather, sceneRequirement);
        } catch (Exception e) {
            log.warn("衣柜模式 LLM 规划失败，回退规则方案", e);
            return null;
        }
    }

    private OutfitRenderPlanResult tryPlanFreeGenerateByLlm(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement) {
        try {
            String systemPrompt = "你是一名专业穿搭顾问。请严格输出 JSON，不要输出 markdown，不要输出解释。";
            String userPrompt = buildFreePlanningPrompt(profile, analysis, weather, sceneRequirement);
            String response = llmService.chat(systemPrompt, userPrompt, 0.5D);
            return parseFreePlanResponse(response, analysis, weather, sceneRequirement);
        } catch (Exception e) {
            log.warn("自由模式 LLM 规划失败，回退规则方案", e);
            return null;
        }
    }

    private OutfitRenderPlanResult fallbackWardrobePlan(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement,
            List<WardrobeItem> wardrobeItems) {
        List<OutfitRenderTaskItemDTO> selectedItems = pickWardrobeItems(wardrobeItems);
        String scene = normalizeScene(sceneRequirement);
        String style = defaultStyleKeywords(profile);
        return OutfitRenderPlanResult.builder()
                .planTitle(scene + "参考穿法")
                .planSummary(String.format("优先从现有衣柜里选出更适合%s和%s天气的一套组合。", scene, weatherSummary(weather)))
                .planReason(String.format("结合你的日常风格%s、当前天气%s，以及现有衣物的分类与颜色，优先挑出更容易直接出门的一套。", style, weatherSummary(weather)))
                .comfortTip(buildComfortTip(weather))
                .styleKeywords(style)
                .sceneKeywords(scene + ",日常真实穿着,贴近现有衣物")
                .renderPrompt(buildWardrobeRenderPrompt(profile, analysis, weather, sceneRequirement, selectedItems))
                .profileAnalysisSummary(buildAnalysisSummary(analysis))
                .planJson(toJson(Map.of("strategy", "fallback_wardrobe", "scene", scene, "style", style)))
                .items(selectedItems)
                .build();
    }

    private OutfitRenderPlanResult fallbackFreePlan(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement) {
        String scene = normalizeScene(sceneRequirement);
        String style = defaultStyleKeywords(profile);
        List<OutfitRenderTaskItemDTO> items = new ArrayList<>();
        items.add(buildConceptItem("TOP", "上装建议", weather.getTemperatureMax() != null && weather.getTemperatureMax() >= 28 ? "轻薄透气上装" : "利落层次上装"));
        items.add(buildConceptItem("BOTTOM", "下装建议", "修饰比例的高腰下装"));
        items.add(buildConceptItem("SHOES", "鞋履建议", "适合长时间活动的轻便鞋履"));

        return OutfitRenderPlanResult.builder()
                .planTitle(scene + "参考造型")
                .planSummary(String.format("根据你的资料、%s天气和%s场景，先给出一套更稳妥的整体方向。", weatherSummary(weather), scene))
                .planReason(String.format("优先考虑你的日常风格%s、体型比例和出行场景，让整体看起来更协调，也更贴近日常可落地穿法。", style))
                .comfortTip(buildComfortTip(weather))
                .styleKeywords(style)
                .sceneKeywords(scene + ",人物全身参考,真实日常场景")
                .renderPrompt(buildFreeRenderPrompt(profile, analysis, weather, sceneRequirement, style))
                .profileAnalysisSummary(buildAnalysisSummary(analysis))
                .planJson(toJson(Map.of("strategy", "fallback_free", "scene", scene, "style", style)))
                .items(items)
                .build();
    }

    private OutfitRenderPlanResult parseWardrobePlanResponse(
            String response,
            List<WardrobeItem> wardrobeItems,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement) {
        JsonNode root = readJson(response);
        if (root == null) {
            return null;
        }

        Map<Long, WardrobeItem> itemMap = wardrobeItems.stream()
                .collect(Collectors.toMap(WardrobeItem::getId, item -> item, (left, right) -> left));

        List<OutfitRenderTaskItemDTO> items = new ArrayList<>();
        JsonNode selectedItems = root.path("selectedItems");
        if (selectedItems.isArray()) {
            for (JsonNode node : selectedItems) {
                Long itemId = node.path("itemId").isNumber() ? node.path("itemId").asLong() : null;
                WardrobeItem item = itemId == null ? null : itemMap.get(itemId);
                if (item == null) {
                    continue;
                }
                items.add(toTaskItem(item, textOrNull(node, "itemRole"), "WARDROBE"));
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        String styleKeywords = textOrDefault(root, "styleKeywords", "日常协调");
        return OutfitRenderPlanResult.builder()
                .planTitle(textOrDefault(root, "planTitle", normalizeScene(sceneRequirement) + "参考穿法"))
                .planSummary(textOrDefault(root, "planSummary", "已结合现有衣物整理出一套更适合当前场景的穿法。"))
                .planReason(textOrDefault(root, "planReason", "已根据天气、场景与现有衣物信息做出筛选。"))
                .comfortTip(textOrDefault(root, "comfortTip", buildComfortTip(weather)))
                .styleKeywords(styleKeywords)
                .sceneKeywords(textOrDefault(root, "sceneKeywords", normalizeScene(sceneRequirement)))
                .renderPrompt(textOrDefault(root, "renderPrompt", buildWardrobeRenderPrompt(null, analysis, weather, sceneRequirement, items)))
                .profileAnalysisSummary(buildAnalysisSummary(analysis))
                .planJson(extractJsonString(response))
                .items(items)
                .build();
    }

    private OutfitRenderPlanResult parseFreePlanResponse(String response, UserProfileAnalysisResult analysis, WeatherSnapshotDTO weather, String sceneRequirement) {
        JsonNode root = readJson(response);
        if (root == null) {
            return null;
        }

        List<OutfitRenderTaskItemDTO> items = new ArrayList<>();
        JsonNode suggestedItems = root.path("suggestedItems");
        if (suggestedItems.isArray()) {
            for (JsonNode node : suggestedItems) {
                items.add(OutfitRenderTaskItemDTO.builder()
                        .itemRole(textOrDefault(node, "itemRole", "ITEM"))
                        .name(textOrDefault(node, "name", "建议单品"))
                        .summaryText(textOrDefault(node, "summaryText", null))
                        .sourceType("CONCEPT")
                        .build());
            }
        }

        return OutfitRenderPlanResult.builder()
                .planTitle(textOrDefault(root, "planTitle", normalizeScene(sceneRequirement) + "参考造型"))
                .planSummary(textOrDefault(root, "planSummary", "已结合天气和个人资料整理出一套参考方向。"))
                .planReason(textOrDefault(root, "planReason", "已根据你的风格与场景需求进行平衡。"))
                .comfortTip(textOrDefault(root, "comfortTip", buildComfortTip(weather)))
                .styleKeywords(textOrDefault(root, "styleKeywords", "日常协调"))
                .sceneKeywords(textOrDefault(root, "sceneKeywords", normalizeScene(sceneRequirement)))
                .renderPrompt(textOrDefault(root, "renderPrompt", "生成一张贴合用户本人气质和场景的全身穿搭参考图"))
                .profileAnalysisSummary(buildAnalysisSummary(analysis))
                .planJson(extractJsonString(response))
                .items(items)
                .build();
    }

    private String buildWardrobePlanningPrompt(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement,
            List<WardrobeItem> wardrobeItems) {
        String itemText = wardrobeItems.stream()
                .limit(40)
                .sorted(Comparator.comparing(WardrobeItem::getId))
                .map(item -> String.format(Locale.ROOT,
                        "{\"itemId\":%d,\"name\":\"%s\",\"categoryLevel1\":\"%s\",\"categoryLevel2\":\"%s\",\"color\":\"%s\",\"styleTags\":\"%s\",\"summaryText\":\"%s\"}",
                        item.getId(),
                        safe(item.getName()),
                        safe(item.getCategoryLevel1()),
                        safe(item.getCategoryLevel2()),
                        safe(item.getColor()),
                        safe(item.getStyleTags()),
                        safe(item.getSummaryText())))
                .collect(Collectors.joining(",\n"));

        return "请根据用户资料、天气、场景，从衣柜候选中选出一套更合适的搭配。\n"
                + "输出 JSON 格式："
                + "{\"planTitle\":\"\",\"planSummary\":\"\",\"planReason\":\"\",\"comfortTip\":\"\",\"styleKeywords\":\"\",\"sceneKeywords\":\"\",\"renderPrompt\":\"\",\"selectedItems\":[{\"itemId\":1,\"itemRole\":\"TOP\"}]}\n"
                + "用户资料：" + buildProfileSummary(profile) + "\n"
                + "人物画像摘要：" + buildAnalysisSummary(analysis) + "\n"
                + "天气：" + weatherSummary(weather) + "\n"
                + "场景：" + normalizeScene(sceneRequirement) + "\n"
                + "衣柜候选：\n[\n" + itemText + "\n]\n"
                + "注意：最终目标不是把某张图上的衣服直接替换到人物身上，而是重新生成一张新的人物穿搭参考图。衣柜图片只是版型、颜色、材质和风格参考。";
    }

    private String buildFreePlanningPrompt(UserProfileDTO profile, UserProfileAnalysisResult analysis, WeatherSnapshotDTO weather, String sceneRequirement) {
        return "请根据用户资料、天气、场景，输出一套不依赖现有衣柜的穿搭建议。\n"
                + "输出 JSON 格式："
                + "{\"planTitle\":\"\",\"planSummary\":\"\",\"planReason\":\"\",\"comfortTip\":\"\",\"styleKeywords\":\"\",\"sceneKeywords\":\"\",\"renderPrompt\":\"\",\"suggestedItems\":[{\"itemRole\":\"TOP\",\"name\":\"\",\"summaryText\":\"\"}]}\n"
                + "用户资料：" + buildProfileSummary(profile) + "\n"
                + "人物画像摘要：" + buildAnalysisSummary(analysis) + "\n"
                + "天气：" + weatherSummary(weather) + "\n"
                + "场景：" + normalizeScene(sceneRequirement) + "\n"
                + "注意：最终目标是重新生成一张新的人物穿搭参考图，而不是修改现有照片。";
    }

    private List<OutfitRenderTaskItemDTO> pickWardrobeItems(List<WardrobeItem> wardrobeItems) {
        List<OutfitRenderTaskItemDTO> result = new ArrayList<>();
        WardrobeItem top = firstMatch(wardrobeItems, "上装");
        WardrobeItem bottom = firstMatch(wardrobeItems, "下装");
        WardrobeItem dress = firstMatch(wardrobeItems, "连衣裙");
        WardrobeItem shoes = firstMatch(wardrobeItems, "鞋子");
        WardrobeItem outer = firstMatch(wardrobeItems, "外套");

        if (dress != null) {
            result.add(toTaskItem(dress, "DRESS", "WARDROBE"));
        } else {
            if (top != null) {
                result.add(toTaskItem(top, "TOP", "WARDROBE"));
            }
            if (bottom != null) {
                result.add(toTaskItem(bottom, "BOTTOM", "WARDROBE"));
            }
        }
        if (outer != null) {
            result.add(toTaskItem(outer, "OUTER", "WARDROBE"));
        }
        if (shoes != null) {
            result.add(toTaskItem(shoes, "SHOES", "WARDROBE"));
        }

        if (result.isEmpty()) {
            wardrobeItems.stream().limit(3).forEach(item -> result.add(toTaskItem(item, "ITEM", "WARDROBE")));
        }
        return result;
    }

    private WardrobeItem firstMatch(List<WardrobeItem> items, String categoryLevel1) {
        return items.stream()
                .filter(item -> categoryLevel1.equals(item.getCategoryLevel1()))
                .findFirst()
                .orElse(null);
    }

    private OutfitRenderTaskItemDTO toTaskItem(WardrobeItem item, String role, String sourceType) {
        return OutfitRenderTaskItemDTO.builder()
                .itemId(item.getId())
                .itemRole(role)
                .name(item.getName())
                .imageUrl(item.getImageUrl())
                .categoryLevel1(item.getCategoryLevel1())
                .categoryLevel2(item.getCategoryLevel2())
                .color(item.getColor())
                .summaryText(item.getSummaryText())
                .sourceType(sourceType)
                .build();
    }

    private OutfitRenderTaskItemDTO buildConceptItem(String role, String name, String summaryText) {
        return OutfitRenderTaskItemDTO.builder()
                .itemRole(role)
                .name(name)
                .summaryText(summaryText)
                .sourceType("CONCEPT")
                .build();
    }

    private String buildProfileSummary(UserProfileDTO profile) {
        return String.format("性别=%s, 身高=%s, 体重=%s, 风格=%s, 备注=%s",
                safe(profile == null ? null : profile.getGender()),
                profile == null || profile.getHeightCm() == null ? "未知" : profile.getHeightCm() + "cm",
                profile == null || profile.getWeightKg() == null ? "未知" : profile.getWeightKg() + "kg",
                safe(profile == null ? null : profile.getDailyStyle()),
                safe(profile == null ? null : profile.getBodyNote()));
    }

    private String buildWardrobeRenderPrompt(
            UserProfileDTO profile,
            UserProfileAnalysisResult analysis,
            WeatherSnapshotDTO weather,
            String sceneRequirement,
            List<OutfitRenderTaskItemDTO> items) {
        String itemSummary = items.stream()
                .map(item -> String.format("%s:%s(%s %s)",
                        safe(item.getItemRole()),
                        safe(item.getName()),
                        safe(item.getCategoryLevel1()),
                        safe(item.getColor())))
                .collect(Collectors.joining("; "));
        return String.format("基于用户的面部照片、正面全身照、侧面全身照和人物画像摘要，重新生成一张新的人物全身穿搭参考图。场景=%s，天气=%s。需要参考这些衣物的版型、颜色、材质和风格：%s。人物脸型、五官、气质和身形比例要尽量贴近用户本人，但不要把任何一张现有照片当作底图直接修改。背景应符合场景，画面真实自然。人物画像摘要=%s",
                normalizeScene(sceneRequirement), weatherSummary(weather), itemSummary, buildAnalysisSummary(analysis));
    }

    private String buildFreeRenderPrompt(UserProfileDTO profile, UserProfileAnalysisResult analysis, WeatherSnapshotDTO weather, String sceneRequirement, String style) {
        return String.format("基于用户的面部照片、正面全身照、侧面全身照和身体信息，重新生成一张新的人物全身穿搭参考图。场景=%s，天气=%s，整体风格=%s。人物脸型、五官、气质、身形比例要尽量贴近用户本人，但不要直接修改现有照片。背景应贴合场景，画面真实自然。人物画像摘要=%s",
                normalizeScene(sceneRequirement), weatherSummary(weather), style, buildAnalysisSummary(analysis));
    }

    private String buildAnalysisSummary(UserProfileAnalysisResult analysis) {
        if (analysis == null) {
            return "五官、身形和气质以真实自然为主，优先生成贴近本人且可落地的穿搭参考图";
        }
        return String.format("face=%s; body=%s; temperament=%s; style=%s; fit=%s; color=%s; scene=%s",
                safe(analysis.getFaceFeatures()),
                safe(analysis.getBodyFeatures()),
                safe(analysis.getTemperament()),
                safe(analysis.getStyleDirection()),
                safe(analysis.getFitSuggestions()),
                safe(analysis.getColorSuggestions()),
                safe(analysis.getSceneSuggestions()));
    }

    private String weatherSummary(WeatherSnapshotDTO weather) {
        return String.format("%s %s~%s度 湿度%s%%",
                safe(weather == null ? null : weather.getWeatherCondition()),
                weather == null || weather.getTemperatureMin() == null ? "?" : weather.getTemperatureMin(),
                weather == null || weather.getTemperatureMax() == null ? "?" : weather.getTemperatureMax(),
                weather == null || weather.getHumidity() == null ? "?" : weather.getHumidity());
    }

    private String buildComfortTip(WeatherSnapshotDTO weather) {
        if (weather != null && weather.getTemperatureMax() != null && weather.getTemperatureMax() >= 28) {
            return "午后偏热，建议优先轻薄透气，也可以保留一层易穿脱外搭。";
        }
        if (weather != null && weather.getTemperatureMin() != null && weather.getTemperatureMin() <= 15) {
            return "早晚偏凉，建议兼顾保暖和层次感。";
        }
        return "整体以舒适和场景适配优先，出门前可再看一眼实时天气。";
    }

    private String defaultStyleKeywords(UserProfileDTO profile) {
        if (profile == null || profile.getDailyStyle() == null || profile.getDailyStyle().isBlank()) {
            return "简洁,协调,真实日常";
        }
        return profile.getDailyStyle().trim() + ",协调,真实日常";
    }

    private String normalizeScene(String sceneRequirement) {
        return sceneRequirement == null || sceneRequirement.isBlank() ? "日常出门" : sceneRequirement.trim();
    }

    private JsonNode readJson(String raw) {
        try {
            String json = extractJsonString(raw);
            if (json == null || json.isBlank()) {
                return null;
            }
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJsonString(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String textOrDefault(JsonNode node, String fieldName, String defaultValue) {
        String value = textOrNull(node, fieldName);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "未提供";
        }
        return value.replace("\"", "'").trim();
    }
}
