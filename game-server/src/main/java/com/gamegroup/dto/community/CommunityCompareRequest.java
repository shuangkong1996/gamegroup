package com.gamegroup.dto.community;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class CommunityCompareRequest {
    @NotEmpty(message = "对比小区不能为空")
    @Size(min = 2, max = 3, message = "MVP 仅支持 2 到 3 个小区对比")
    private List<Long> ids;

    private String city;
}
