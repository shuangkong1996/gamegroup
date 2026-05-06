package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.community.CommunityCompareCardDTO;
import com.gamegroup.dto.community.CommunityCompareRequest;
import com.gamegroup.dto.community.CommunityCompareResponse;
import com.gamegroup.dto.community.CommunityMetricGroupDTO;
import com.gamegroup.dto.community.CommunityMetricItemDTO;
import com.gamegroup.dto.community.CommunitySearchItemDTO;
import com.gamegroup.dto.community.MortgageReferenceDTO;
import com.gamegroup.dto.mortgage.MortgageCalculateRequest;
import com.gamegroup.dto.mortgage.MortgageCalculateResponse;
import com.gamegroup.dto.mortgage.MortgageRepaymentMethod;
import com.gamegroup.entity.Community;
import com.gamegroup.repository.CommunityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private static final int ENABLED_STATUS = 1;
    private static final BigDecimal DEFAULT_AREA_SQM = new BigDecimal("90");
    private static final BigDecimal DEFAULT_DOWN_PAYMENT_RATIO = new BigDecimal("0.30");
    private static final Integer DEFAULT_MORTGAGE_YEARS = 30;
    private static final DateTimeFormatter UPDATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CommunityRepository communityRepository;
    private final MortgageCalculatorService mortgageCalculatorService;

    public List<CommunitySearchItemDTO> search(String keyword, String city) {
        List<Community> communities =
                communityRepository.search(normalize(keyword), normalize(city), PageRequest.of(0, 20));
        return communities.stream()
                .map(
                        c ->
                                CommunitySearchItemDTO.builder()
                                        .id(c.getId())
                                        .name(c.getName())
                                        .city(c.getCity())
                                        .district(c.getDistrict())
                                        .coverImage(c.getCoverImage())
                                        .listingAvgPrice(c.getListingAvgPrice())
                                        .updateTime(c.getUpdateTime())
                                        .build())
                .toList();
    }

    public CommunityCompareResponse compare(CommunityCompareRequest request) {
        List<Long> ids = request.getIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.size() < 2 || ids.size() > 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "MVP 仅支持 2 到 3 个小区对比");
        }

        List<Community> rows = communityRepository.findByIdInAndStatus(ids, ENABLED_STATUS);
        Map<Long, Community> rowMap = new HashMap<>();
        for (Community row : rows) {
            rowMap.put(row.getId(), row);
        }

        List<Community> ordered = new ArrayList<>();
        for (Long id : ids) {
            Community c = rowMap.get(id);
            if (c == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "小区不存在或已下线: " + id);
            }
            if (request.getCity() != null
                    && !request.getCity().isBlank()
                    && !request.getCity().equals(c.getCity())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "小区不在指定城市: " + c.getName());
            }
            ordered.add(c);
        }

        List<CommunityCompareCardDTO> cards =
                ordered.stream()
                        .map(
                                c ->
                                        CommunityCompareCardDTO.builder()
                                                .id(c.getId())
                                                .name(c.getName())
                                                .city(c.getCity())
                                                .district(c.getDistrict())
                                                .coverImage(c.getCoverImage())
                                                .listingAvgPrice(c.getListingAvgPrice())
                                                .updateTime(c.getUpdateTime())
                                                .mortgageReference(buildMortgageReference(c))
                                                .build())
                        .toList();

        return CommunityCompareResponse.builder()
                .defaultAreaSqm(DEFAULT_AREA_SQM)
                .defaultDownPaymentRatio(DEFAULT_DOWN_PAYMENT_RATIO)
                .dataUpdatedAt(pickLatestUpdateTime(ordered))
                .dataDisclaimer("数据仅供参考，实际以成交信息和官方政策为准。")
                .communities(cards)
                .groups(buildGroups(ordered))
                .build();
    }

    private String pickLatestUpdateTime(List<Community> communities) {
        LocalDateTime latest = null;
        for (Community community : communities) {
            if (community.getUpdateTime() == null) {
                continue;
            }
            if (latest == null || community.getUpdateTime().isAfter(latest)) {
                latest = community.getUpdateTime();
            }
        }
        if (latest == null) {
            return "--";
        }
        return latest.format(UPDATE_TIME_FORMATTER);
    }

    private List<CommunityMetricGroupDTO> buildGroups(List<Community> communities) {
        return List.of(
                CommunityMetricGroupDTO.builder()
                        .key("market")
                        .label("交易动态")
                        .items(
                                List.of(
                                        metric(
                                                "listingAvgPrice",
                                                "挂牌均价",
                                                "元/㎡",
                                                "LOWER_BETTER",
                                                communities,
                                                c -> formatDecimal(c.getListingAvgPrice())),
                                        metric(
                                                "priceChangePct1y",
                                                "近一年涨跌幅",
                                                "%",
                                                "NONE",
                                                communities,
                                                c -> formatDecimal(c.getPriceChangePct1y())),
                                        metric(
                                                "showingCountRecent",
                                                "近期带看",
                                                "次",
                                                "HIGHER_BETTER",
                                                communities,
                                                c -> formatNumber(c.getShowingCountRecent())),
                                        metric(
                                                "onsaleCount",
                                                "在售房源",
                                                "套",
                                                "HIGHER_BETTER",
                                                communities,
                                                c -> formatNumber(c.getOnsaleCount()))))
                        .build(),
                CommunityMetricGroupDTO.builder()
                        .key("base")
                        .label("基础信息")
                        .items(
                                List.of(
                                        metric(
                                                "builtYear",
                                                "建成年代",
                                                "年",
                                                "NEWER_BETTER",
                                                communities,
                                                c -> formatNumber(c.getBuiltYear())),
                                        metric("developer", "开发商", "", "NONE", communities, Community::getDeveloper),
                                        metric(
                                                "propertyCompany",
                                                "物业公司",
                                                "",
                                                "NONE",
                                                communities,
                                                Community::getPropertyCompany),
                                        metric(
                                                "buildingType",
                                                "建筑类型",
                                                "",
                                                "NONE",
                                                communities,
                                                Community::getBuildingType)))
                        .build(),
                CommunityMetricGroupDTO.builder()
                        .key("quality")
                        .label("居住品质")
                        .items(
                                List.of(
                                        metric(
                                                "plotRatio",
                                                "容积率",
                                                "",
                                                "LOWER_BETTER",
                                                communities,
                                                c -> formatDecimal(c.getPlotRatio())),
                                        metric(
                                                "greeningRate",
                                                "绿化率",
                                                "%",
                                                "HIGHER_BETTER",
                                                communities,
                                                c -> formatDecimal(c.getGreeningRate())),
                                        metric(
                                                "parkingRatio",
                                                "车位配比",
                                                "",
                                                "HIGHER_BETTER",
                                                communities,
                                                c -> formatDecimal(c.getParkingRatio()))))
                        .build(),
                CommunityMetricGroupDTO.builder()
                        .key("cost")
                        .label("费用成本")
                        .items(
                                List.of(
                                        metric(
                                                "propertyFee",
                                                "物业费",
                                                "元/㎡·月",
                                                "LOWER_BETTER",
                                                communities,
                                                c -> formatDecimal(c.getPropertyFee())),
                                        metric(
                                                "subwayDistanceMeters",
                                                "最近地铁距离",
                                                "米",
                                                "LOWER_BETTER",
                                                communities,
                                                c -> formatNumber(c.getSubwayDistanceMeters())),
                                        metric(
                                                "walkToRailMinutes",
                                                "步行轻轨",
                                                "分钟",
                                                "LOWER_BETTER",
                                                communities,
                                                c -> formatNumber(c.getWalkToRailMinutes()))))
                        .build(),
                CommunityMetricGroupDTO.builder()
                        .key("external")
                        .label("外部资源")
                        .items(
                                List.of(
                                        metric(
                                                "primarySchool",
                                                "对应小学",
                                                "",
                                                "NONE",
                                                communities,
                                                Community::getPrimarySchool),
                                        metric(
                                                "middleSchool",
                                                "对应中学",
                                                "",
                                                "NONE",
                                                communities,
                                                Community::getMiddleSchool)))
                        .build());
    }

    private CommunityMetricItemDTO metric(
            String field,
            String label,
            String unit,
            String betterDirection,
            List<Community> communities,
            java.util.function.Function<Community, String> extractor) {
        List<String> values = communities.stream().map(extractor).map(this::safeText).toList();
        return CommunityMetricItemDTO.builder()
                .field(field)
                .label(label)
                .unit(unit)
                .betterDirection(betterDirection)
                .values(values)
                .build();
    }

    private MortgageReferenceDTO buildMortgageReference(Community community) {
        if (community.getListingAvgPrice() == null || community.getListingAvgPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return MortgageReferenceDTO.builder()
                    .areaSqm(DEFAULT_AREA_SQM)
                    .downPaymentRatio(DEFAULT_DOWN_PAYMENT_RATIO)
                    .totalPrice(BigDecimal.ZERO)
                    .downPayment(BigDecimal.ZERO)
                    .loanPrincipal(BigDecimal.ZERO)
                    .monthlyPayment(BigDecimal.ZERO)
                    .build();
        }
        BigDecimal totalPrice = community.getListingAvgPrice().multiply(DEFAULT_AREA_SQM).setScale(2, RoundingMode.HALF_UP);
        BigDecimal downPayment = totalPrice.multiply(DEFAULT_DOWN_PAYMENT_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal principal = totalPrice.subtract(downPayment).setScale(2, RoundingMode.HALF_UP);

        MortgageCalculateRequest request = new MortgageCalculateRequest();
        request.setYears(DEFAULT_MORTGAGE_YEARS);
        request.setRepaymentMethod(MortgageRepaymentMethod.EQUAL_INSTALLMENT);
        request.setCommercialPrincipal(principal);
        request.setProvidentFundPrincipal(BigDecimal.ZERO);

        MortgageCalculateResponse result = mortgageCalculatorService.calculate(request);
        BigDecimal monthlyPayment =
                result.getCombined() == null || result.getCombined().getFirstMonthPayment() == null
                        ? BigDecimal.ZERO
                        : result.getCombined().getFirstMonthPayment().setScale(2, RoundingMode.HALF_UP);

        return MortgageReferenceDTO.builder()
                .areaSqm(DEFAULT_AREA_SQM)
                .downPaymentRatio(DEFAULT_DOWN_PAYMENT_RATIO)
                .totalPrice(totalPrice)
                .downPayment(downPayment)
                .loanPrincipal(principal)
                .monthlyPayment(monthlyPayment)
                .build();
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatNumber(Integer value) {
        if (value == null) {
            return "--";
        }
        return String.valueOf(value);
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "--";
        }
        return value;
    }
}
