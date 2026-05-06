package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.community.CommunityCompareRequest;
import com.gamegroup.dto.community.CommunityCompareResponse;
import com.gamegroup.dto.community.CommunitySearchItemDTO;
import com.gamegroup.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "小区对比助手")
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "搜索小区")
    @GetMapping("/search")
    public ApiResponse<List<CommunitySearchItemDTO>> search(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String city) {
        return ApiResponse.success(communityService.search(keyword, city));
    }

    @Operation(summary = "对比小区")
    @PostMapping("/compare")
    public ApiResponse<CommunityCompareResponse> compare(@Valid @RequestBody CommunityCompareRequest request) {
        return ApiResponse.success(communityService.compare(request));
    }
}
