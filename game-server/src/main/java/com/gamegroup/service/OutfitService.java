package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.AdoptOutfitRequest;
import com.gamegroup.dto.DailyOutfitDTO;
import com.gamegroup.dto.OutfitItemDTO;
import com.gamegroup.dto.OutfitRecordDTO;
import com.gamegroup.dto.WeatherSnapshotDTO;
import com.gamegroup.entity.OutfitPlan;
import com.gamegroup.entity.OutfitPlanItem;
import com.gamegroup.entity.OutfitRecord;
import com.gamegroup.entity.WardrobeItem;
import com.gamegroup.repository.OutfitPlanItemRepository;
import com.gamegroup.repository.OutfitPlanRepository;
import com.gamegroup.repository.OutfitRecordRepository;
import com.gamegroup.repository.WardrobeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutfitService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OutfitPlanRepository outfitPlanRepository;
    private final OutfitPlanItemRepository outfitPlanItemRepository;
    private final OutfitRecordRepository outfitRecordRepository;
    private final WardrobeItemRepository wardrobeItemRepository;
    private final WeatherService weatherService;

    @Transactional
    public DailyOutfitDTO getDailyOutfit(Long userId, String targetDateText) {
        LocalDate targetDate = parseTargetDate(targetDateText);
        String preferredCity = weatherService.getPreferredCity(userId, null);
        return outfitPlanRepository.findFirstByUserIdAndTargetDateAndStatusOrderByIdDesc(userId, targetDate, "GENERATED")
                .filter(plan -> preferredCity.equals(plan.getWeatherCity()))
                .map(this::toDailyOutfit)
                .orElseGet(() -> generateDailyOutfit(userId, targetDate));
    }

    @Transactional
    public DailyOutfitDTO refreshDailyOutfit(Long userId, String targetDateText) {
        LocalDate targetDate = parseTargetDate(targetDateText);
        outfitPlanRepository.findFirstByUserIdAndTargetDateAndStatusOrderByIdDesc(userId, targetDate, "GENERATED")
                .ifPresent(plan -> {
                    plan.setStatus("DISMISSED");
                    plan.setUpdateTime(LocalDateTime.now());
                    outfitPlanRepository.save(plan);
                });
        return generateDailyOutfit(userId, targetDate);
    }

    @Transactional
    public void adoptOutfit(Long userId, Long planId, AdoptOutfitRequest request) {
        OutfitPlan plan = outfitPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WARDROBE_OUTFIT_PLAN_NOT_FOUND));
        plan.setStatus("ADOPTED");
        plan.setUpdateTime(LocalDateTime.now());
        outfitPlanRepository.save(plan);

        OutfitRecord record = outfitRecordRepository.findByUserIdAndPlanId(userId, planId).orElseGet(OutfitRecord::new);
        record.setUserId(userId);
        record.setPlanId(planId);
        record.setTargetDate(plan.getTargetDate());
        record.setFeedback(request == null ? null : request.getFeedback());
        record.setUserNote(request == null ? null : request.getUserNote());
        outfitRecordRepository.save(record);
    }

    public List<OutfitRecordDTO> getRecords(Long userId) {
        Map<Long, OutfitPlan> planMap = outfitPlanRepository.findByUserIdOrderByTargetDateDescIdDesc(userId).stream()
                .collect(Collectors.toMap(OutfitPlan::getId, Function.identity(), (left, right) -> left));

        return outfitRecordRepository.findByUserIdOrderByTargetDateDescIdDesc(userId).stream()
                .map(record -> {
                    OutfitPlan plan = planMap.get(record.getPlanId());
                    return OutfitRecordDTO.builder()
                            .id(record.getId())
                            .planId(record.getPlanId())
                            .targetDate(record.getTargetDate().format(DATE_FORMATTER))
                            .planTitle(plan == null ? null : plan.getPlanTitle())
                            .planSummary(plan == null ? null : plan.getPlanSummary())
                            .weatherCondition(plan == null ? null : plan.getWeatherCondition())
                            .temperatureMin(plan == null ? null : plan.getTemperatureMin())
                            .temperatureMax(plan == null ? null : plan.getTemperatureMax())
                            .feedback(record.getFeedback())
                            .createTime(record.getCreateTime() == null ? null : record.getCreateTime().format(DATE_TIME_FORMATTER))
                            .build();
                })
                .toList();
    }

    private DailyOutfitDTO generateDailyOutfit(Long userId, LocalDate targetDate) {
        List<WardrobeItem> availableItems = wardrobeItemRepository.findByUserIdAndStatusAndRecognizeStatusOrderByIdDesc(userId, "ACTIVE", "SUCCESS")
                .stream()
                .filter(this::matchWeatherRule)
                .sorted(Comparator.comparing(WardrobeItem::getId))
                .toList();
        if (availableItems.size() < 2) {
            throw new BusinessException(ErrorCode.WARDROBE_NO_AVAILABLE_ITEMS);
        }

        WeatherSnapshotDTO weather = weatherService.getTomorrowWeather(userId, null);
        OutfitPlan plan = new OutfitPlan();
        plan.setUserId(userId);
        plan.setTargetDate(targetDate);
        plan.setWeatherCity(weather.getCity());
        plan.setWeatherCondition(weather.getWeatherCondition());
        plan.setTemperatureMin(weather.getTemperatureMin());
        plan.setTemperatureMax(weather.getTemperatureMax());
        plan.setHumidity(weather.getHumidity());
        plan.setPlanTitle("明日轻松搭配");
        plan.setPlanSummary(buildSummary(weather));
        plan.setPlanReason(buildReason(weather));
        plan.setComfortTip(buildTip(weather));
        OutfitPlan savedPlan = outfitPlanRepository.save(plan);

        List<WardrobeItem> selected = pickItems(availableItems);
        List<OutfitPlanItem> planItems = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            WardrobeItem item = selected.get(i);
            OutfitPlanItem planItem = new OutfitPlanItem();
            planItem.setPlanId(savedPlan.getId());
            planItem.setWardrobeItemId(item.getId());
            planItem.setItemRole(resolveRole(item, i));
            planItems.add(planItem);
        }
        outfitPlanItemRepository.saveAll(planItems);
        return toDailyOutfit(savedPlan);
    }

    private DailyOutfitDTO toDailyOutfit(OutfitPlan plan) {
        List<OutfitPlanItem> planItems = outfitPlanItemRepository.findByPlanIdOrderByIdAsc(plan.getId());
        List<Long> itemIds = planItems.stream().map(OutfitPlanItem::getWardrobeItemId).toList();
        Map<Long, WardrobeItem> itemMap = wardrobeItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(WardrobeItem::getId, Function.identity(), (left, right) -> left));
        List<OutfitItemDTO> items = planItems.stream().map(planItem -> {
            WardrobeItem item = itemMap.get(planItem.getWardrobeItemId());
            return OutfitItemDTO.builder()
                    .itemId(planItem.getWardrobeItemId())
                    .itemRole(planItem.getItemRole())
                    .name(item == null ? null : item.getName())
                    .imageUrl(item == null ? null : item.getImageUrl())
                    .categoryLevel1(item == null ? null : item.getCategoryLevel1())
                    .color(item == null ? null : item.getColor())
                    .build();
        }).toList();

        return DailyOutfitDTO.builder()
                .planId(plan.getId())
                .targetDate(plan.getTargetDate().format(DATE_FORMATTER))
                .weatherCity(plan.getWeatherCity())
                .weatherCondition(plan.getWeatherCondition())
                .temperatureMin(plan.getTemperatureMin())
                .temperatureMax(plan.getTemperatureMax())
                .humidity(plan.getHumidity())
                .planTitle(plan.getPlanTitle())
                .planSummary(plan.getPlanSummary())
                .planReason(plan.getPlanReason())
                .comfortTip(plan.getComfortTip())
                .status(plan.getStatus())
                .items(items)
                .build();
    }

    private boolean matchWeatherRule(WardrobeItem item) {
        return item.getRecognizeStatus() != null && "SUCCESS".equals(item.getRecognizeStatus());
    }

    private List<WardrobeItem> pickItems(List<WardrobeItem> items) {
        List<WardrobeItem> tops = items.stream().filter(item -> "上装".equals(item.getCategoryLevel1())).toList();
        List<WardrobeItem> bottoms = items.stream().filter(item -> "下装".equals(item.getCategoryLevel1())).toList();
        List<WardrobeItem> others = items.stream().filter(item -> !"上装".equals(item.getCategoryLevel1()) && !"下装".equals(item.getCategoryLevel1())).toList();

        List<WardrobeItem> selected = new ArrayList<>();
        if (!tops.isEmpty()) {
            selected.add(tops.get(0));
        }
        if (!bottoms.isEmpty()) {
            selected.add(bottoms.get(0));
        }
        if (!others.isEmpty()) {
            selected.add(others.get(0));
        }
        if (selected.size() < 2) {
            selected = items.stream().limit(Math.min(3, items.size())).toList();
        }
        return selected;
    }

    private String resolveRole(WardrobeItem item, int index) {
        if ("上装".equals(item.getCategoryLevel1())) {
            return "TOP";
        }
        if ("下装".equals(item.getCategoryLevel1())) {
            return "BOTTOM";
        }
        if ("鞋子".equals(item.getCategoryLevel1())) {
            return "SHOES";
        }
        return index == 0 ? "TOP" : "ACCESSORY";
    }

    private String buildSummary(WeatherSnapshotDTO weather) {
        return String.format("%s %s~%s 度，给你整理了一套轻松日常搭配", weather.getWeatherCondition(), weather.getTemperatureMin(), weather.getTemperatureMax());
    }

    private String buildReason(WeatherSnapshotDTO weather) {
        return String.format("结合次日%s、%s~%s 度和湿度 %s%% 的情况，优先选择更适合日常出门的衣物组合。",
                weather.getWeatherCondition(), weather.getTemperatureMin(), weather.getTemperatureMax(), weather.getHumidity());
    }

    private String buildTip(WeatherSnapshotDTO weather) {
        return weather.getTemperatureMax() != null && weather.getTemperatureMax() >= 28
                ? "午后偏热，可准备轻薄外搭或注意防晒"
                : "早晚温差可能更明显，出门前可再看一眼实时天气";
    }

    private LocalDate parseTargetDate(String targetDateText) {
        if (targetDateText == null || targetDateText.isBlank()) {
            return LocalDate.now().plusDays(1);
        }
        return LocalDate.parse(targetDateText, DATE_FORMATTER);
    }
}
