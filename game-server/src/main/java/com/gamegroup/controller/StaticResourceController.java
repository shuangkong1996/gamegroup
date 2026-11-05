package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.StaticResourceDTO;
import com.gamegroup.service.StaticResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "静态资源接口")
@RestController
@RequestMapping("/static-resources")
@RequiredArgsConstructor
public class StaticResourceController {

    private final StaticResourceService staticResourceService;

    @Operation(summary = "按分组获取静态资源")
    @GetMapping("/{groupKey}")
    public ApiResponse<List<StaticResourceDTO>> getByGroup(@PathVariable String groupKey) {
        return ApiResponse.success(staticResourceService.getByGroup(groupKey));
    }
}
