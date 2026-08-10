package com.gamegroup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.AdoptOutfitRenderTaskRequest;
import com.gamegroup.dto.CreateOutfitRenderTaskRequest;
import com.gamegroup.dto.OutfitRenderPlanResult;
import com.gamegroup.dto.OutfitRenderTaskDTO;
import com.gamegroup.dto.OutfitRenderTaskItemDTO;
import com.gamegroup.dto.UserProfileAnalysisResult;
import com.gamegroup.dto.UserProfileDTO;
import com.gamegroup.dto.WeatherSnapshotDTO;
import com.gamegroup.entity.OutfitRecord;
import com.gamegroup.entity.OutfitRenderTask;
import com.gamegroup.entity.WardrobeItem;
import com.gamegroup.repository.OutfitRecordRepository;
import com.gamegroup.repository.OutfitRenderTaskRepository;
import com.gamegroup.repository.WardrobeItemRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitRenderTaskService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OutfitRenderTaskRepository outfitRenderTaskRepository;
    private final OutfitRecordRepository outfitRecordRepository;
    private final WardrobeItemRepository wardrobeItemRepository;
    private final AuthService authService;
    private final WeatherService weatherService;
    private final OutfitPlanningService outfitPlanningService;
    private final OutfitRenderAsyncService outfitRenderAsyncService;
    private final UserProfileAnalysisService userProfileAnalysisService;

    @Transactional
    public OutfitRenderTaskDTO createTask(Long userId, CreateOutfitRenderTaskRequest request) {
        UserProfileDTO profile = authService.getUserProfile(userId);
        validateProfile(profile);

        String resolvedMode = resolveMode(request);
        String sceneRequirement = normalizeSceneRequirement(request.getSceneRequirement());
        LocalDate targetDate = parseTargetDate(request.getTargetDate());
        WeatherSnapshotDTO weather = weatherService.getTomorrowWeather(userId, request.getCity());
        log.info("开始创建参考搭配任务: userId={}, mode={}, scene={}, city={}, targetDate={}", userId, resolvedMode, sceneRequirement, weather.getCity(), targetDate);
        UserProfileAnalysisResult analysis = userProfileAnalysisService.analyze(profile);
        log.info("用户形象摘要分析完成: userId={}, summary={}", userId, analysis == null ? "null" : analysis.getSummaryText());

        OutfitRenderTask task = new OutfitRenderTask();
        task.setUserId(userId);
        task.setMode(resolvedMode);
        task.setStatus("PLANNING");
        task.setTargetDate(targetDate);
        task.setWeatherCity(weather.getCity());
        task.setWeatherCondition(weather.getWeatherCondition());
        task.setTemperatureMin(weather.getTemperatureMin());
        task.setTemperatureMax(weather.getTemperatureMax());
        task.setHumidity(weather.getHumidity());
        task.setSceneRequirement(sceneRequirement);
        task.setUseWardrobeItems("WARDROBE_BASED".equals(resolvedMode));
        outfitRenderTaskRepository.save(task);

        try {
            OutfitRenderPlanResult planResult;
            if ("WARDROBE_BASED".equals(resolvedMode)) {
                List<WardrobeItem> wardrobeItems = wardrobeItemRepository
                        .findByUserIdAndStatusAndRecognizeStatusOrderByIdDesc(userId, "ACTIVE", "SUCCESS");
                if (wardrobeItems.isEmpty()) {
                    throw new BusinessException(ErrorCode.WARDROBE_NO_AVAILABLE_ITEMS);
                }
                planResult = outfitPlanningService.planWithWardrobe(profile, analysis, weather, sceneRequirement, wardrobeItems);
            } else {
                planResult = outfitPlanningService.planFreeGenerate(profile, analysis, weather, sceneRequirement);
            }

            applyPlan(task, planResult);
            log.info("参考搭配方案整理完成: taskId={}, mode={}, itemCount={}", task.getId(), task.getMode(), planResult.getItems() == null ? 0 : planResult.getItems().size());
            task.setStatus("QUEUED");
            task.setProvider("BAILIAN");
            outfitRenderTaskRepository.save(task);
            outfitRenderAsyncService.generateRenderImageAsync(task.getId(), profile, analysis, planResult.getItems());
            log.info("参考图任务已入队，等待异步生成: taskId={}", task.getId());
        } catch (BusinessException e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            outfitRenderTaskRepository.save(task);
            throw e;
        } catch (Exception e) {
            log.error("生成参考方案任务失败: userId={}, mode={}", userId, resolvedMode, e);
            task.setStatus("FAILED");
            task.setErrorMessage("参考方案生成失败，请稍后再试");
            outfitRenderTaskRepository.save(task);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        return toTaskDTO(task, userId);
    }

    public OutfitRenderTaskDTO getTask(Long userId, Long taskId) {
        OutfitRenderTask task = outfitRenderTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTFIT_RENDER_TASK_NOT_FOUND));
        refreshTaskStatus(task);
        return toTaskDTO(task, userId);
    }

    public List<OutfitRenderTaskDTO> getRecentTasks(Long userId) {
        return outfitRenderTaskRepository.findTop10ByUserIdOrderByIdDesc(userId).stream()
                .map(task -> toTaskDTO(task, userId))
                .toList();
    }

    public OutfitRenderTaskDTO getLatestTask(Long userId) {
        return outfitRenderTaskRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(task -> {
                    refreshTaskStatus(task);
                    return toTaskDTO(task, userId);
                })
                .orElse(null);
    }

    @Transactional
    public void adoptTask(Long userId, Long taskId, AdoptOutfitRenderTaskRequest request) {
        OutfitRenderTask task = outfitRenderTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTFIT_RENDER_TASK_NOT_FOUND));
        refreshTaskStatus(task);
        if (!"SUCCESS".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请先等待参考图生成完成");
        }

        OutfitRecord record = outfitRecordRepository.findByUserIdAndRenderTaskId(userId, taskId).orElseGet(OutfitRecord::new);
        record.setUserId(userId);
        record.setPlanId(0L);
        record.setRenderTaskId(taskId);
        record.setTargetDate(task.getTargetDate());
        record.setFeedback(request == null ? null : request.getFeedback());
        record.setUserNote(request == null ? null : request.getUserNote());
        record.setReferenceImageUrl(task.getResultImageUrl());
        outfitRecordRepository.save(record);
        log.info("参考搭配已采纳: userId={}, taskId={}, mode={}", userId, taskId, task.getMode());
    }

    private void refreshTaskStatus(OutfitRenderTask task) {
        if (task == null) {
            return;
        }
        if (!"QUEUED".equals(task.getStatus()) && !"RENDERING".equals(task.getStatus())) {
            return;
        }
    }

    private void applyPlan(OutfitRenderTask task, OutfitRenderPlanResult planResult) {
        task.setPlanTitle(planResult.getPlanTitle());
        task.setPlanSummary(planResult.getPlanSummary());
        task.setPlanReason(planResult.getPlanReason());
        task.setComfortTip(planResult.getComfortTip());
        task.setStyleKeywords(planResult.getStyleKeywords());
        task.setSceneKeywords(planResult.getSceneKeywords());
        task.setRenderPrompt(planResult.getRenderPrompt());
        task.setPlanJson(planResult.getPlanJson());
        task.setSelectedItemIdsJson(toSelectedItemsJson(planResult.getItems()));
    }

    private OutfitRenderTaskDTO toTaskDTO(OutfitRenderTask task, Long userId) {
        List<OutfitRenderTaskItemDTO> items = resolveItems(task, userId);
        return OutfitRenderTaskDTO.builder()
                .taskId(task.getId())
                .mode(task.getMode())
                .status(task.getStatus())
                .targetDate(task.getTargetDate() == null ? null : task.getTargetDate().format(DATE_FORMATTER))
                .weatherCity(task.getWeatherCity())
                .weatherCondition(task.getWeatherCondition())
                .temperatureMin(task.getTemperatureMin())
                .temperatureMax(task.getTemperatureMax())
                .humidity(task.getHumidity())
                .sceneRequirement(task.getSceneRequirement())
                .useWardrobeItems(task.getUseWardrobeItems())
                .planTitle(task.getPlanTitle())
                .planSummary(task.getPlanSummary())
                .planReason(task.getPlanReason())
                .comfortTip(task.getComfortTip())
                .styleKeywords(task.getStyleKeywords())
                .sceneKeywords(task.getSceneKeywords())
                .resultImageUrl(task.getResultImageUrl())
                .errorMessage(task.getErrorMessage())
                .items(items)
                .build();
    }

    private List<OutfitRenderTaskItemDTO> resolveItems(OutfitRenderTask task, Long userId) {
        if (task.getPlanJson() == null || task.getPlanJson().isBlank()) {
            return List.of();
        }

        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(task.getPlanJson(), new TypeReference<Map<String, Object>>() { });
            if (root.containsKey("suggestedItems")) {
                return OBJECT_MAPPER.convertValue(root.get("suggestedItems"), new TypeReference<List<OutfitRenderTaskItemDTO>>() { });
            }
        } catch (Exception ignored) {
        }

        List<OutfitRenderTaskItemDTO> savedItems = parseSelectedItems(task.getSelectedItemIdsJson());
        if (!savedItems.isEmpty()) {
            return savedItems;
        }
        return List.of();
    }

    private List<OutfitRenderTaskItemDTO> parseSelectedItems(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(rawJson, new TypeReference<List<OutfitRenderTaskItemDTO>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toSelectedItemsJson(List<OutfitRenderTaskItemDTO> items) {
        try {
            return OBJECT_MAPPER.writeValueAsString(items == null ? List.of() : items);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void validateProfile(UserProfileDTO profile) {
        if (profile == null
                || !Boolean.TRUE.equals(profile.getBasicInfoCompleted())
                || !Boolean.TRUE.equals(profile.getImageInfoCompleted())) {
            throw new BusinessException(ErrorCode.OUTFIT_RENDER_PROFILE_INCOMPLETE);
        }
    }

    private String resolveMode(CreateOutfitRenderTaskRequest request) {
        String mode = request == null ? null : request.getMode();
        if (mode == null || mode.isBlank()) {
            return Boolean.FALSE.equals(request == null ? null : request.getUseWardrobeItems())
                    ? "FREE_GENERATE"
                    : "WARDROBE_BASED";
        }
        String normalized = mode.trim().toUpperCase();
        if (!"WARDROBE_BASED".equals(normalized) && !"FREE_GENERATE".equals(normalized)) {
            return "WARDROBE_BASED";
        }
        return normalized;
    }

    private String normalizeSceneRequirement(String sceneRequirement) {
        if (sceneRequirement == null || sceneRequirement.isBlank()) {
            return "日常出门";
        }
        return sceneRequirement.trim();
    }

    private LocalDate parseTargetDate(String targetDate) {
        if (targetDate == null || targetDate.isBlank()) {
            return LocalDate.now().plusDays(1);
        }
        return LocalDate.parse(targetDate.trim(), DATE_FORMATTER);
    }
}
