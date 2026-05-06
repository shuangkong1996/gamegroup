package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeOverviewDTO {

    private long totalCount;

    private long pendingCount;

    private long successCount;

    private List<WardrobeItemDTO> recentItems;
}
