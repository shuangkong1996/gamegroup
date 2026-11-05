package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.CreateWeddingRsvpRequest;
import com.gamegroup.service.WeddingRsvpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "婚礼请帖接口")
@RestController
@RequestMapping("/v1/wedding/invitations")
@RequiredArgsConstructor
public class WeddingRsvpController {

    private final WeddingRsvpService weddingRsvpService;

    @Operation(summary = "提交婚礼出席回复")
    @PostMapping("/{slug}/rsvps")
    public ApiResponse<Void> createRsvp(
            @PathVariable String slug,
            @Valid @RequestBody CreateWeddingRsvpRequest request) {
        weddingRsvpService.create(slug, request);
        return ApiResponse.success();
    }
}
