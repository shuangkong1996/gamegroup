package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.BoxDTO;
import com.gamegroup.dto.BoxDetailDTO;
import com.gamegroup.dto.BoxItemDTO;
import com.gamegroup.dto.BoxRevealDTO;
import com.gamegroup.entity.Box;
import com.gamegroup.entity.BoxItem;
import com.gamegroup.entity.BoxItem.ItemQuality;
import com.gamegroup.repository.BoxItemRepository;
import com.gamegroup.repository.BoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class BoxService {

    private final BoxRepository boxRepository;
    private final BoxItemRepository boxItemRepository;

    private static final int BOX_WIDTH = 20;
    private static final int BOX_MIN_HEIGHT = 100;
    private static final int BOX_MAX_HEIGHT = 150;

    private static final int ITEM_MIN_SIZE = 1;
    private static final int ITEM_MAX_SIZE = 4;

    private static final BigDecimal FILL_MIN = new BigDecimal("0.50");
    private static final BigDecimal FILL_MAX = new BigDecimal("0.90");

    public BoxService(BoxRepository boxRepository, BoxItemRepository boxItemRepository) {
        this.boxRepository = boxRepository;
        this.boxItemRepository = boxItemRepository;
    }

    @Transactional
    public BoxDetailDTO generateBox() {
        Random random = new Random();
        int height = BOX_MIN_HEIGHT + random.nextInt(BOX_MAX_HEIGHT - BOX_MIN_HEIGHT + 1);
        BigDecimal fillRate = randomFillRate(random);

        Box box = new Box();
        box.setWidth(BOX_WIDTH);
        box.setHeight(height);
        box.setFillRate(fillRate);
        box.setRevealedQualities(generateRevealedQualities(random));

        List<BoxItem> items = generateItems(box, random);
        BigDecimal totalValue = BigDecimal.ZERO;
        for (BoxItem item : items) {
            totalValue = totalValue.add(item.getValue());
        }
        box.setTotalValue(totalValue);
        box.setStartingPrice(calculateStartingPrice(box.getRevealedQualities()));

        Box savedBox = boxRepository.save(box);
        items.forEach(item -> item.setBoxId(savedBox.getId()));
        boxItemRepository.saveAll(items);

        return toDetailDTO(savedBox, items);
    }

    public BoxDetailDTO getBox(Long boxId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOX_NOT_FOUND));
        List<BoxItem> items = boxItemRepository.findByBoxId(boxId);
        return toDetailDTO(box, items);
    }

    public BoxRevealDTO getRevealedBox(Long boxId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOX_NOT_FOUND));
        BoxRevealDTO revealDTO = new BoxRevealDTO();
        revealDTO.setId(box.getId());
        revealDTO.setWidth(box.getWidth());
        revealDTO.setHeight(box.getHeight());
        revealDTO.setStartingPrice(box.getStartingPrice());
        revealDTO.setFillRate(box.getFillRate());
        revealDTO.setRevealedQualities(box.getRevealedQualities());
        return revealDTO;
    }

    private List<BoxItem> generateItems(Box box, Random random) {
        int totalCells = box.getWidth() * box.getHeight();
        int targetCells = new BigDecimal(totalCells)
                .multiply(box.getFillRate())
                .setScale(0, RoundingMode.DOWN)
                .intValue();

        List<BoxItem> items = new ArrayList<>();
        int usedCells = 0;

        while (usedCells < targetCells) {
            int width = ITEM_MIN_SIZE + random.nextInt(ITEM_MAX_SIZE - ITEM_MIN_SIZE + 1);
            int height = ITEM_MIN_SIZE + random.nextInt(ITEM_MAX_SIZE - ITEM_MIN_SIZE + 1);
            int cells = width * height;
            if (usedCells + cells > targetCells) {
                break;
            }

            BoxItem item = new BoxItem();
            item.setName(pickItemName(random));
            item.setQuality(pickQuality(random));
            item.setValue(randomValue(item.getQuality(), random));
            item.setWidth(width);
            item.setHeight(height);
            item.setPositionX(random.nextInt(box.getWidth()));
            item.setPositionY(random.nextInt(box.getHeight() / 2));
            items.add(item);
            usedCells += cells;
        }
        return items;
    }

    private String generateRevealedQualities(Random random) {
        int count = 3 + random.nextInt(4);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(pickQuality(random).name());
        }
        return builder.toString();
    }

    private BigDecimal calculateStartingPrice(String revealedQualities) {
        if (revealedQualities == null || revealedQualities.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String[] parts = revealedQualities.split(",");
        BigDecimal sum = BigDecimal.ZERO;
        for (String part : parts) {
            ItemQuality quality = ItemQuality.valueOf(part);
            sum = sum.add(averagePrice(quality));
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal randomFillRate(Random random) {
        BigDecimal diff = FILL_MAX.subtract(FILL_MIN);
        BigDecimal fraction = new BigDecimal(random.nextDouble())
                .setScale(2, RoundingMode.HALF_UP);
        return FILL_MIN.add(diff.multiply(fraction)).setScale(2, RoundingMode.HALF_UP);
    }

    private ItemQuality pickQuality(Random random) {
        int roll = random.nextInt(100);
        if (roll < 50) {
            return ItemQuality.COMMON;
        }
        if (roll < 75) {
            return ItemQuality.GOOD;
        }
        if (roll < 90) {
            return ItemQuality.RARE;
        }
        if (roll < 98) {
            return ItemQuality.EPIC;
        }
        return ItemQuality.LEGENDARY;
    }

    private BigDecimal averagePrice(ItemQuality quality) {
        return switch (quality) {
            case COMMON -> new BigDecimal("252.50");
            case GOOD -> new BigDecimal("500.00");
            case RARE -> new BigDecimal("1250.00");
            case EPIC -> new BigDecimal("3000.00");
            case LEGENDARY -> new BigDecimal("6500.00");
        };
    }

    private BigDecimal randomValue(ItemQuality quality, Random random) {
        int min;
        int max;
        switch (quality) {
            case COMMON -> {
                min = 5;
                max = 500;
            }
            case GOOD -> {
                min = 200;
                max = 800;
            }
            case RARE -> {
                min = 500;
                max = 2000;
            }
            case EPIC -> {
                min = 1000;
                max = 5000;
            }
            default -> {
                min = 3000;
                max = 10000;
            }
        }
        int value = min + random.nextInt(max - min + 1);
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String pickItemName(Random random) {
        String[] items = {
                "陶片", "铜钉", "石削片", "粗麻织片", "兽骨碎片",
                "瓦当残片", "木楔", "石珠", "灰陶碗片", "破损香炉脚",
                "青铜钺残片", "玉坠", "漆器盖", "陶鼎耳", "纹饰铜片",
                "青铜箭镞", "刻纹石牌", "竹简片段", "漆木盒盖", "纹样砖块",
                "青铜剑", "玉璧（小）", "错金铜镜", "青瓷壶", "鎏金饰件",
                "铜壶", "祭祀石刻", "兽面纹铜片", "玉珠串", "铜铃",
                "玉璧（大）", "鎏金铜镜（完整）", "金箔饰片", "古代编钟（小型）", "铜鼎（中型）",
                "祭祀玉杖", "玉佩组", "漆器礼盒（完整）", "金丝嵌玉饰", "青铜礼器套件",
                "王族冠饰", "国玺印", "祭天玉璧（完整）", "编钟（大型）", "金面具",
                "皇室礼器套装", "传世玉玺", "龙纹青铜鼎（大型）", "帝王陪葬金器套件", "大型祭祀器组合"
        };
        return items[random.nextInt(items.length)];
    }

    private BoxDetailDTO toDetailDTO(Box box, List<BoxItem> items) {
        BoxDTO boxDTO = new BoxDTO();
        boxDTO.setId(box.getId());
        boxDTO.setWidth(box.getWidth());
        boxDTO.setHeight(box.getHeight());
        boxDTO.setTotalValue(box.getTotalValue());
        boxDTO.setStartingPrice(box.getStartingPrice());
        boxDTO.setFillRate(box.getFillRate());
        boxDTO.setRevealedQualities(box.getRevealedQualities());

        List<BoxItemDTO> itemDTOs = new ArrayList<>();
        for (BoxItem item : items) {
            BoxItemDTO itemDTO = new BoxItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setBoxId(item.getBoxId());
            itemDTO.setName(item.getName());
            itemDTO.setQuality(item.getQuality().name());
            itemDTO.setValue(item.getValue());
            itemDTO.setWidth(item.getWidth());
            itemDTO.setHeight(item.getHeight());
            itemDTO.setPositionX(item.getPositionX());
            itemDTO.setPositionY(item.getPositionY());
            itemDTOs.add(itemDTO);
        }

        BoxDetailDTO detailDTO = new BoxDetailDTO();
        detailDTO.setBox(boxDTO);
        detailDTO.setItems(itemDTOs);
        return detailDTO;
    }
}
