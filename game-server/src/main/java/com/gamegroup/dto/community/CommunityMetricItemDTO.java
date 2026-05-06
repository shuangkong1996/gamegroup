package com.gamegroup.dto.community;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommunityMetricItemDTO {
    private String field;
    private String label;
    private String unit;
    private String betterDirection;
    private List<String> values;
}
