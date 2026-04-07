package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.CreateLedgerRecurringRuleRequest;
import com.gamegroup.dto.LedgerRecurringRuleDTO;
import com.gamegroup.dto.UpdateLedgerRecurringRuleStatusRequest;
import com.gamegroup.service.LedgerRecurringRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "周期账接口")
@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class LedgerRecurringRuleController {

    private final LedgerRecurringRuleService recurringRuleService;

    @Operation(summary = "创建周期账规则")
    @PostMapping("/recurring-rules")
    public ApiResponse<LedgerRecurringRuleDTO> createRule(
            @Valid @RequestBody CreateLedgerRecurringRuleRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(recurringRuleService.createRule(userId, request));
    }

    @Operation(summary = "获取周期账规则列表")
    @GetMapping("/recurring-rules")
    public ApiResponse<List<LedgerRecurringRuleDTO>> listRules(
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(recurringRuleService.listRules(userId));
    }

    @Operation(summary = "启用/暂停周期账规则")
    @PutMapping("/recurring-rules/{id}/status")
    public ApiResponse<LedgerRecurringRuleDTO> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateLedgerRecurringRuleStatusRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(recurringRuleService.updateStatus(userId, id, request));
    }
}

