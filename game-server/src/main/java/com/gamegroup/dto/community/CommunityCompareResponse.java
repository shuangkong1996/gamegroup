package com.gamegroup.dto.community;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommunityCompareResponse {
    private BigDecimal defaultAreaSqm;
    private BigDecimal defaultDownPaymentRatio;
    private String dataUpdatedAt;
    private String dataDisclaimer;
    private List<CommunityCompareCardDTO> communities;
    private List<CommunityMetricGroupDTO> groups;
}
