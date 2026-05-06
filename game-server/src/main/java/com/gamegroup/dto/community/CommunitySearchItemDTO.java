package com.gamegroup.dto.community;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommunitySearchItemDTO {
    private Long id;
    private String name;
    private String city;
    private String district;
    private String coverImage;
    private BigDecimal listingAvgPrice;
    private LocalDateTime updateTime;
}
