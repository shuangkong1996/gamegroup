package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaticResourceDTO {

    private String groupKey;

    private String resourceKey;

    private String resourceName;

    private String resourceUrl;

    private Integer sortOrder;
}
