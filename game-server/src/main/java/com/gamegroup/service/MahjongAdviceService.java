package com.gamegroup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.MahjongAdviceResponse;
import com.gamegroup.service.mahjong.MahjongHandAnalysis;
import com.gamegroup.service.mahjong.MahjongHandAnalyzer;
import com.gamegroup.service.mahjong.MahjongTileCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * 麻将胡牌提示：视觉模型只负责识牌，听/胡由规则引擎计算。
 */
@Slf4j
@Service
public class MahjongAdviceService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ANALYSIS_MODE = "vision_recognize_plus_rule_engine";

    private final LlmService llmService;
    private final MinioService minioService;
    private final MahjongHandAnalyzer mahjongHandAnalyzer;

    public MahjongAdviceService(
            LlmService llmService,
            MinioService minioService,
            MahjongHandAnalyzer mahjongHandAnalyzer) {
        this.llmService = llmService;
        this.minioService = minioService;
        this.mahjongHandAnalyzer = mahjongHandAnalyzer;
    }

    public MahjongAdviceResponse analyzeUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.MAHJONG_IMAGE_EMPTY);
        }
        String fileName = minioService.uploadFile(file);
        String imageUrl = minioService.getFileUrl(fileName);
        return analyzePhoto(imageUrl, fileName);
    }

    public MahjongAdviceResponse analyzeTiles(List<String> tiles) {
        MahjongHandAnalysis analysis = mahjongHandAnalyzer.analyze(tiles);
        return buildResponse(
                null,
                analysis,
                analysis.isValid() ? "高" : "低",
                false,
                analysis.isValid() ? "手牌由手动/接口输入，未走图像识别" : "手牌输入无法完成规则分析",
                null);
    }

    public MahjongAdviceResponse analyzePhoto(String imageUrl, String imageFileName) {
        String imageDataUrl = buildImageDataUrl(imageUrl, imageFileName);
        if (imageDataUrl == null || imageDataUrl.isBlank()) {
            throw new BusinessException(ErrorCode.MAHJONG_IMAGE_EMPTY);
        }

        String raw = llmService.chatWithImageForJson(
                buildSystemPrompt(),
                buildUserPrompt(),
                imageDataUrl,
                llmService.getImageModel(),
                0.0D);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.MAHJONG_ANALYZE_FAILED);
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(stripCodeFence(raw));
            List<String> recognizedTiles = extractRecognizedTiles(node);
            String visionConfidence = normalizeConfidence(text(node, "confidenceLabel", "中"));
            boolean uncertain = booleanValue(node, "uncertain");
            String visionNote = text(node, "note", "");

            MahjongHandAnalysis analysis = mahjongHandAnalyzer.analyze(recognizedTiles);
            return buildResponse(imageUrl, analysis, visionConfidence, uncertain, visionNote, stripCodeFence(raw));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("麻将识别结果解析失败", e);
            throw new BusinessException(ErrorCode.MAHJONG_ANALYZE_FAILED);
        }
    }

    private MahjongAdviceResponse buildResponse(
            String imageUrl,
            MahjongHandAnalysis analysis,
            String visionConfidence,
            boolean uncertain,
            String visionNote,
            String rawResult) {
        List<String> tiles = analysis.getRecognizedTiles();
        String handSummary = buildHandSummary(tiles, analysis.getTileCount());
        String confidenceLabel = resolveConfidence(analysis, visionConfidence, uncertain);
        String suggestion = mergeSuggestion(analysis, uncertain, visionNote);

        return new MahjongAdviceResponse(
                imageUrl,
                handSummary,
                analysis.isAlreadyWinning(),
                analysis.getWinningTiles(),
                tiles,
                analysis.getTileCount(),
                analysis.isValid(),
                analysis.getHandForm(),
                ANALYSIS_MODE,
                suggestion,
                confidenceLabel,
                rawResult);
    }

    private String buildHandSummary(List<String> tiles, int tileCount) {
        if (tiles == null || tiles.isEmpty()) {
            return "未能识别出手牌";
        }
        return "识别到手牌 " + tileCount + " 张：" + String.join("、", tiles);
    }

    private String resolveConfidence(MahjongHandAnalysis analysis, String visionConfidence, boolean uncertain) {
        if (!analysis.isValid() || uncertain) {
            return "低";
        }
        if (analysis.getTileCount() != 13 && analysis.getTileCount() != 14) {
            return "低";
        }
        if ("高".equals(visionConfidence) || "中".equals(visionConfidence) || "低".equals(visionConfidence)) {
            return visionConfidence;
        }
        return "中";
    }

    private String mergeSuggestion(MahjongHandAnalysis analysis, boolean uncertain, String visionNote) {
        StringBuilder builder = new StringBuilder();
        if (analysis.getSuggestion() != null && !analysis.getSuggestion().isBlank()) {
            builder.append(analysis.getSuggestion().trim());
        }
        if (uncertain) {
            appendSentence(builder, "图片存在遮挡/反光/不全，识牌结果仅供参考，建议补拍");
        }
        if (visionNote != null && !visionNote.isBlank()) {
            appendSentence(builder, visionNote.trim());
        }
        if (!analysis.isValid()) {
            appendSentence(builder, "听胡结论需在识别张数正确后才可靠");
        } else {
            appendSentence(builder, "听胡结论由规则引擎计算，不依赖模型自由发挥");
        }
        return builder.toString();
    }

    private void appendSentence(StringBuilder builder, String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return;
        }
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '。') {
            builder.append('。');
        }
        builder.append(sentence.trim());
        if (builder.charAt(builder.length() - 1) != '。') {
            builder.append('。');
        }
    }

    private List<String> extractRecognizedTiles(JsonNode node) {
        List<String> tiles = new ArrayList<>();

        JsonNode tilesNode = node.get("tiles");
        if (tilesNode != null && tilesNode.isArray()) {
            tilesNode.forEach(item -> {
                if (item == null || item.isNull()) {
                    return;
                }
                if (item.isTextual()) {
                    addNormalizedTile(tiles, item.asText());
                    return;
                }
                if (item.isObject()) {
                    String name = firstNonBlank(
                            text(item, "name", null),
                            text(item, "tile", null),
                            text(item, "label", null));
                    int count = Math.max(1, item.path("count").asInt(1));
                    for (int i = 0; i < count && i < MahjongTileCodec.MAX_SAME_TILE; i++) {
                        addNormalizedTile(tiles, name);
                    }
                }
            });
        }

        // 兼容模型输出 tileCounts: {"一万":2,"二筒":1}
        JsonNode tileCountsNode = node.get("tileCounts");
        if (tiles.isEmpty() && tileCountsNode != null && tileCountsNode.isObject()) {
            tileCountsNode.fields().forEachRemaining(entry -> {
                int count = entry.getValue() == null ? 0 : entry.getValue().asInt(0);
                for (int i = 0; i < count && i < MahjongTileCodec.MAX_SAME_TILE; i++) {
                    addNormalizedTile(tiles, entry.getKey());
                }
            });
        }

        // 兼容模型只给 handSummary 文本的弱结果
        if (tiles.isEmpty()) {
            tiles.addAll(extractTilesFromText(text(node, "handSummary", "")));
        }

        try {
            return MahjongTileCodec.normalizeAndSort(tiles);
        } catch (IllegalArgumentException ex) {
            // 保留可解析部分
            List<String> partial = new ArrayList<>();
            for (String tile : tiles) {
                String normalized = MahjongTileCodec.normalizeName(tile);
                if (normalized != null) {
                    partial.add(normalized);
                }
            }
            return MahjongTileCodec.normalizeAndSort(partial);
        }
    }

    private void addNormalizedTile(List<String> tiles, String raw) {
        String normalized = MahjongTileCodec.normalizeName(raw);
        if (normalized != null) {
            tiles.add(normalized);
        }
    }

    private List<String> extractTilesFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        // 按标准牌名最长匹配扫描
        List<String> names = MahjongTileCodec.standardNames();
        String source = text.replaceAll("\\s+", "");
        int i = 0;
        while (i < source.length()) {
            boolean matched = false;
            for (String name : names) {
                if (source.startsWith(name, i)) {
                    found.add(name);
                    i += name.length();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // 尝试 1万 / 3筒 / 5条
                if (i + 1 < source.length()) {
                    String maybe = source.substring(i, i + 2);
                    String normalized = MahjongTileCodec.normalizeName(maybe);
                    if (normalized != null) {
                        found.add(normalized);
                        i += 2;
                        matched = true;
                    }
                }
            }
            if (!matched) {
                i++;
            }
        }
        return found;
    }

    private String buildSystemPrompt() {
        return "你是麻将识牌器，不是胡牌判断器。"
                + "你的唯一任务：从照片中识别用户手牌，并逐张列出。"
                + "严格返回单个 JSON 对象，不要 markdown，不要解释。"
                + "字段仅包含：tiles, tileCount, uncertain, confidenceLabel, note。"
                + "tiles 必须是字符串数组，按从左到右、从近到远的可见顺序，逐张列出每一张牌。"
                + "同一张牌出现几次，tiles 中就必须重复写几次。例如三张一万要写三次\"一万\"。"
                + "tiles 元素只能使用标准牌名："
                + String.join("、", MahjongTileCodec.standardNames()) + "。"
                + "禁止使用 1万、1筒、1条、幺鸡、中、发、白、东、南、西、北、五索、五饼 之类别名。"
                + "tileCount 必须等于 tiles.length。"
                + "不要判断是否听牌或胡牌，不要输出 winningTiles/alreadyWinning。"
                + "只统计用户手中的牌，忽略牌河、打出的牌、牌背、手指、桌面装饰、计分器。"
                + "若有横放/侧放/刚摸到的牌，只要属于手牌也要计入。"
                + "看不清的牌不要猜测填入；若整体把握不足，uncertain=true，并在 note 简短说明原因。"
                + "confidenceLabel 仅可取 高、中、低。"
                + "常见封闭手牌是 13 张（听牌）或 14 张（摸牌后）。若可见张数明显不是这些，仍按实际可见张数输出，不要凑数。";
    }

    private String buildUserPrompt() {
        return "请识别这张照片中的麻将手牌。"
                + "先在心里按花色清点：万、筒、条、字各有多少张，再输出 tiles。"
                + "输出前自检：1) tiles 是否每张都标准牌名 2) tileCount 是否等于 tiles 长度 3) 任何单牌是否超过 4 张。"
                + "再次强调：不要计算听口，不要判断胡牌，只识牌。";
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
            log.error("读取麻将手牌图片失败: imageUrl={}, imageFileName={}", imageUrl, imageFileName, e);
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

        String lowerSource = source.toLowerCase(Locale.ROOT);
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
        if (node == null) {
            return defaultValue;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String result = value.asText();
        return result == null || result.isBlank() ? defaultValue : result.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() && value.asBoolean(false);
    }

    private String normalizeConfidence(String value) {
        if ("高".equals(value) || "中".equals(value) || "低".equals(value)) {
            return value;
        }
        return "中";
    }
}
