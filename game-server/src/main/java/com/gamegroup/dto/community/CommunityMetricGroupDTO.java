package com.gamegroup.dto.community;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommunityMetricGroupDTO {
    private String key;
    private String label;
    private List<CommunityMetricItemDTO> items;
}
