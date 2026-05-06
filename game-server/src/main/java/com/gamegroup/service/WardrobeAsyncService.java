package com.gamegroup.service;

import com.gamegroup.entity.WardrobeItem;
import com.gamegroup.entity.WardrobeItemDetail;
import com.gamegroup.repository.WardrobeItemDetailRepository;
import com.gamegroup.repository.WardrobeItemRepository;
import com.gamegroup.dto.ClothingRecognitionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WardrobeAsyncService {

    private final WardrobeItemRepository wardrobeItemRepository;
    private final WardrobeItemDetailRepository wardrobeItemDetailRepository;
    private final ClothingRecognitionService clothingRecognitionService;

    @Async
    @Transactional
    public void recognizeWardrobeItem(Long itemId) {
        wardrobeItemRepository.findById(itemId).ifPresentOrElse(this::processItem, () ->
                log.warn("异步整理衣物时未找到记录: itemId={}", itemId));
    }

    private void processItem(WardrobeItem item) {
        try {
            item.setRecognizeStatus("PROCESSING");
            item.setRecognizeErrorMsg(null);
            item.setUpdateTime(LocalDateTime.now());
            wardrobeItemRepository.save(item);

            ClothingRecognitionResult result = clothingRecognitionService.recognizeFromImage(item.getImageUrl(), item.getImageFileName());
            if (result.isFallback()) {
                item.setRecognizeStatus("FAILED");
                item.setRecognizeErrorMsg(result.getFallbackReason());
                item.setUpdateTime(LocalDateTime.now());
                wardrobeItemRepository.save(item);

                WardrobeItemDetail detail = wardrobeItemDetailRepository.findByItemId(item.getId())
                        .orElseGet(WardrobeItemDetail::new);
                detail.setItemId(item.getId());
                detail.setRecognizedRaw(result.getRawJson());
                detail.setConfidenceScore(BigDecimal.valueOf(result.getConfidenceScore()));
                detail.setUpdateTime(LocalDateTime.now());
                wardrobeItemDetailRepository.save(detail);
                return;
            }

            item.setName(result.getName());
            item.setCategoryLevel1(result.getCategoryLevel1());
            item.setCategoryLevel2(result.getCategoryLevel2());
            item.setColor(result.getColor());
            item.setPatternTags(result.getPatternTags());
            item.setSeasonTags(result.getSeasonTags());
            item.setStyleTags(result.getStyleTags());
            item.setOccasionTags(result.getOccasionTags());
            item.setMaterial(result.getMaterial());
            item.setThicknessLevel(result.getThicknessLevel());
            item.setWarmthLevel(result.getWarmthLevel());
            item.setWaterproofLevel(result.getWaterproofLevel());
            item.setFitType(result.getFitType());
            item.setSummaryText(result.getSummaryText());
            item.setMatchPoints(result.getMatchPoints());
            item.setAvoidPoints(result.getAvoidPoints());
            item.setRecognizeStatus("SUCCESS");
            item.setRecognizeErrorMsg(null);
            item.setUpdateTime(LocalDateTime.now());
            wardrobeItemRepository.save(item);

            WardrobeItemDetail detail = wardrobeItemDetailRepository.findByItemId(item.getId())
                    .orElseGet(WardrobeItemDetail::new);
            detail.setItemId(item.getId());
            detail.setRecognizedRaw(result.getRawJson());
            detail.setConfidenceScore(BigDecimal.valueOf(result.getConfidenceScore()));
            detail.setUpdateTime(LocalDateTime.now());
            wardrobeItemDetailRepository.save(detail);
        } catch (Exception e) {
            log.error("异步整理衣物失败: itemId={}", item.getId(), e);
            item.setRecognizeStatus("FAILED");
            item.setRecognizeErrorMsg("当前信息待补充，可手动完善");
            item.setUpdateTime(LocalDateTime.now());
            wardrobeItemRepository.save(item);
        }
    }
}
