package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.CreateLedgerAccountRequest;
import com.gamegroup.dto.CreateLedgerBatchRecordRequest;
import com.gamegroup.dto.CreateLedgerRecordRequest;
import com.gamegroup.dto.LedgerAccountDTO;
import com.gamegroup.dto.LedgerBatchRecordResponse;
import com.gamegroup.dto.LedgerCategoryGroupDTO;
import com.gamegroup.dto.LedgerCategoryStatsResponse;
import com.gamegroup.dto.LedgerOverviewStatsDTO;
import com.gamegroup.dto.LedgerRecordDTO;
import com.gamegroup.dto.LedgerRecordQueryResponse;
import com.gamegroup.dto.LedgerTrendPointDTO;
import com.gamegroup.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "记账接口")
@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @Operation(summary = "获取账户列表")
    @GetMapping("/accounts")
    public ApiResponse<List<LedgerAccountDTO>> getAccounts(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(ledgerService.getAccounts(userId));
    }

    @Operation(summary = "新增账户")
    @PostMapping("/accounts")
    public ApiResponse<LedgerAccountDTO> createAccount(
            @Valid @RequestBody CreateLedgerAccountRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(ledgerService.createAccount(userId, request));
    }

    @Operation(summary = "获取分类列表")
    @GetMapping("/categories")
    public ApiResponse<LedgerCategoryGroupDTO> getCategories(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(ledgerService.getCategories(userId));
    }

    @Operation(summary = "记一笔")
    @PostMapping("/records")
    public ApiResponse<LedgerRecordDTO> createRecord(
            @Valid @RequestBody CreateLedgerRecordRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(ledgerService.createRecord(userId, request));
    }

    @Operation(summary = "批量记账")
    @PostMapping("/records/batch")
    public ApiResponse<LedgerBatchRecordResponse> createBatchRecords(
            @Valid @RequestBody CreateLedgerBatchRecordRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(ledgerService.createBatchRecords(userId, request));
    }

    @Operation(summary = "删除记录")
    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable("id") Long id,
            @RequestAttribute("userId") Long userId) {
        ledgerService.deleteRecord(userId, id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取记录列表")
    @GetMapping("/records")
    public ApiResponse<LedgerRecordQueryResponse> queryRecords(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(ledgerService.queryRecords(userId, month, startDate, endDate, accountId, categoryId, page, size));
    }

    @Operation(summary = "获取月度总览")
    @GetMapping("/stats/overview")
    public ApiResponse<LedgerOverviewStatsDTO> getOverviewStats(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(ledgerService.getOverviewStats(userId, month, startDate, endDate));
    }

    @Operation(summary = "获取每日收支趋势")
    @GetMapping("/stats/trend")
    public ApiResponse<List<LedgerTrendPointDTO>> getTrendStats(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(ledgerService.getTrendStats(userId, month, startDate, endDate));
    }

    @Operation(summary = "获取分类占比")
    @GetMapping("/stats/category")
    public ApiResponse<LedgerCategoryStatsResponse> getCategoryStats(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(ledgerService.getCategoryStats(userId, month, startDate, endDate));
    }
}
