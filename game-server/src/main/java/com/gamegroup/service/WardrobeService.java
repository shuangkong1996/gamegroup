package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.UpdateWardrobeItemRequest;
import com.gamegroup.dto.WardrobeCategoryGroupDTO;
import com.gamegroup.dto.WardrobeCategoryOverviewDTO;
import com.gamegroup.dto.WardrobeCategorySectionDTO;
import com.gamegroup.dto.WardrobeItemDTO;
import com.gamegroup.dto.WardrobeItemDetailDTO;
import com.gamegroup.dto.WardrobeOverviewDTO;
import com.gamegroup.dto.WardrobeUploadResponse;
import com.gamegroup.entity.WardrobeItem;
import com.gamegroup.entity.WardrobeItemDetail;
import com.gamegroup.repository.WardrobeItemDetailRepository;
import com.gamegroup.repository.WardrobeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WardrobeService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WardrobeItemRepository wardrobeItemRepository;
    private final WardrobeItemDetailRepository wardrobeItemDetailRepository;
    private final MinioService minioService;
    private final WardrobeAsyncService wardrobeAsyncService;

    @Transactional
    public WardrobeUploadResponse uploadItem(Long userId, MultipartFile file) {
        String fileName = minioService.uploadFile(file);
        String imageUrl = minioService.getFileUrl(fileName);

        WardrobeItem item = new WardrobeItem();
        item.setUserId(userId);
        item.setImageFileName(fileName);
        item.setImageUrl(imageUrl);
        item.setRecognizeStatus("PENDING");
        item.setStatus("ACTIVE");
        WardrobeItem saved = wardrobeItemRepository.save(item);

        Long itemId = saved.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    wardrobeAsyncService.recognizeWardrobeItem(itemId);
                }
            });
        } else {
            wardrobeAsyncService.recognizeWardrobeItem(itemId);
        }

        return WardrobeUploadResponse.builder()
                .itemId(itemId)
                .imageUrl(saved.getImageUrl())
                .recognizeStatus(saved.getRecognizeStatus())
                .build();
    }

    public WardrobeOverviewDTO getOverview(Long userId) {
        List<WardrobeItemDTO> recentItems = wardrobeItemRepository.findByUserIdAndStatusOrderByIdDesc(userId, "ACTIVE")
                .stream()
                .limit(10)
                .map(this::toItemDTO)
                .toList();

        return WardrobeOverviewDTO.builder()
                .totalCount(wardrobeItemRepository.countByUserIdAndStatus(userId, "ACTIVE"))
                .pendingCount(wardrobeItemRepository.countByUserIdAndStatusAndRecognizeStatus(userId, "ACTIVE", "PENDING")
                        + wardrobeItemRepository.countByUserIdAndStatusAndRecognizeStatus(userId, "ACTIVE", "PROCESSING"))
                .successCount(wardrobeItemRepository.countByUserIdAndStatusAndRecognizeStatus(userId, "ACTIVE", "SUCCESS"))
                .recentItems(recentItems)
                .build();
    }

    public List<WardrobeItemDTO> getItems(Long userId, String recognizeStatus) {
        List<WardrobeItem> items;
        if (recognizeStatus == null || recognizeStatus.isBlank()) {
            items = wardrobeItemRepository.findByUserIdAndStatusOrderByIdDesc(userId, "ACTIVE");
        } else if ("PENDING".equals(recognizeStatus)) {
            items = wardrobeItemRepository.findByUserIdAndStatusAndRecognizeStatusInOrderByIdDesc(
                    userId,
                    "ACTIVE",
                    List.of("PENDING", "PROCESSING"));
        } else {
            items = wardrobeItemRepository.findByUserIdAndStatusAndRecognizeStatusOrderByIdDesc(userId, "ACTIVE", recognizeStatus);
        }
        return items.stream().map(this::toItemDTO).toList();
    }

    public WardrobeCategoryOverviewDTO getCategoryOverview(Long userId) {
        List<WardrobeItem> items = wardrobeItemRepository.findByUserIdAndStatusOrderByIdDesc(userId, "ACTIVE");
        List<WardrobeCategorySectionDTO> categories = items.stream()
                .collect(Collectors.groupingBy(this::resolveCategoryLevel1))
                .entrySet()
                .stream()
                .map(entry -> toCategorySection(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> compareCategorySection(left, right))
                .toList();

        return WardrobeCategoryOverviewDTO.builder()
                .totalCount((long) items.size())
                .categories(categories)
                .build();
    }

    public WardrobeItemDetailDTO getItemDetail(Long userId, Long itemId) {
        WardrobeItem item = findItem(userId, itemId);
        WardrobeItemDetail detail = wardrobeItemDetailRepository.findByItemId(itemId).orElse(null);
        return toDetailDTO(item, detail);
    }

    @Transactional
    public WardrobeItemDetailDTO updateItem(Long userId, Long itemId, UpdateWardrobeItemRequest request) {
        WardrobeItem item = findItem(userId, itemId);
        item.setName(trim(request.getName()));
        item.setCategoryLevel1(trim(request.getCategoryLevel1()));
        item.setCategoryLevel2(trim(request.getCategoryLevel2()));
        item.setColor(trim(request.getColor()));
        item.setPatternTags(trim(request.getPatternTags()));
        item.setSeasonTags(trim(request.getSeasonTags()));
        item.setStyleTags(trim(request.getStyleTags()));
        item.setOccasionTags(trim(request.getOccasionTags()));
        item.setMaterial(trim(request.getMaterial()));
        item.setThicknessLevel(request.getThicknessLevel());
        item.setWarmthLevel(request.getWarmthLevel());
        item.setWaterproofLevel(request.getWaterproofLevel());
        item.setFitType(trim(request.getFitType()));
        item.setSummaryText(trim(request.getSummaryText()));
        item.setMatchPoints(trim(request.getMatchPoints()));
        item.setAvoidPoints(trim(request.getAvoidPoints()));
        if (!"SUCCESS".equals(item.getRecognizeStatus())) {
            item.setRecognizeStatus("SUCCESS");
            item.setRecognizeErrorMsg(null);
        }

        WardrobeItem saved = wardrobeItemRepository.save(item);
        WardrobeItemDetail detail = wardrobeItemDetailRepository.findByItemId(itemId).orElse(null);
        return toDetailDTO(saved, detail);
    }

    @Transactional
    public void archiveItem(Long userId, Long itemId) {
        WardrobeItem item = findItem(userId, itemId);
        item.setStatus("ARCHIVED");
        wardrobeItemRepository.save(item);
    }

    private WardrobeItem findItem(Long userId, Long itemId) {
        return wardrobeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WARDROBE_ITEM_NOT_FOUND));
    }

    private WardrobeItemDTO toItemDTO(WardrobeItem item) {
        return WardrobeItemDTO.builder()
                .id(item.getId())
                .imageUrl(item.getImageUrl())
                .name(item.getName())
                .categoryLevel1(item.getCategoryLevel1())
                .categoryLevel2(item.getCategoryLevel2())
                .color(item.getColor())
                .seasonTags(item.getSeasonTags())
                .styleTags(item.getStyleTags())
                .recognizeStatus(item.getRecognizeStatus())
                .status(item.getStatus())
                .createTime(item.getCreateTime() == null ? null : item.getCreateTime().format(DATE_TIME_FORMATTER))
                .build();
    }

    private WardrobeItemDetailDTO toDetailDTO(WardrobeItem item, WardrobeItemDetail detail) {
        BigDecimal confidenceScore = detail == null ? null : detail.getConfidenceScore();
        return WardrobeItemDetailDTO.builder()
                .id(item.getId())
                .imageUrl(item.getImageUrl())
                .name(item.getName())
                .categoryLevel1(item.getCategoryLevel1())
                .categoryLevel2(item.getCategoryLevel2())
                .color(item.getColor())
                .patternTags(item.getPatternTags())
                .seasonTags(item.getSeasonTags())
                .styleTags(item.getStyleTags())
                .occasionTags(item.getOccasionTags())
                .material(item.getMaterial())
                .thicknessLevel(item.getThicknessLevel())
                .warmthLevel(item.getWarmthLevel())
                .waterproofLevel(item.getWaterproofLevel())
                .fitType(item.getFitType())
                .summaryText(item.getSummaryText())
                .matchPoints(item.getMatchPoints())
                .avoidPoints(item.getAvoidPoints())
                .recognizeStatus(item.getRecognizeStatus())
                .recognizeErrorMsg(item.getRecognizeErrorMsg())
                .confidenceScore(confidenceScore == null ? null : confidenceScore.doubleValue())
                .build();
    }

    private WardrobeCategorySectionDTO toCategorySection(String categoryName, List<WardrobeItem> items) {
        List<WardrobeCategoryGroupDTO> groups = items.stream()
                .collect(Collectors.groupingBy(this::resolveCategoryLevel2))
                .entrySet()
                .stream()
                .map(entry -> WardrobeCategoryGroupDTO.builder()
                        .name(entry.getKey())
                        .count((long) entry.getValue().size())
                        .items(entry.getValue().stream()
                                .map(this::toItemDTO)
                                .toList())
                        .build())
                .sorted((left, right) -> compareCategoryGroup(left, right))
                .toList();

        List<String> coverImages = items.stream()
                .map(WardrobeItem::getImageUrl)
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        return WardrobeCategorySectionDTO.builder()
                .name(categoryName)
                .count((long) items.size())
                .coverImages(coverImages)
                .groups(groups)
                .build();
    }

    private String resolveCategoryLevel1(WardrobeItem item) {
        return defaultCategory(item.getCategoryLevel1());
    }

    private String resolveCategoryLevel2(WardrobeItem item) {
        return defaultCategory(item.getCategoryLevel2());
    }

    private String defaultCategory(String value) {
        if (value == null || value.isBlank()) {
            return "暂未分类";
        }
        return value.trim();
    }

    private int compareCategorySection(WardrobeCategorySectionDTO left, WardrobeCategorySectionDTO right) {
        int countCompare = Long.compare(safeCount(right.getCount()), safeCount(left.getCount()));
        if (countCompare != 0) {
            return countCompare;
        }
        return compareText(left.getName(), right.getName());
    }

    private int compareCategoryGroup(WardrobeCategoryGroupDTO left, WardrobeCategoryGroupDTO right) {
        int countCompare = Long.compare(safeCount(right.getCount()), safeCount(left.getCount()));
        if (countCompare != 0) {
            return countCompare;
        }
        return compareText(left.getName(), right.getName());
    }

    private long safeCount(Long count) {
        return count == null ? 0L : count;
    }

    private int compareText(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
