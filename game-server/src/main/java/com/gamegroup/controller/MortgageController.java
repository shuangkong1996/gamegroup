package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.mortgage.MortgageCalculateRequest;
import com.gamegroup.dto.mortgage.MortgageCalculateResponse;
import com.gamegroup.dto.mortgage.MortgageDefaultsResponse;
import com.gamegroup.service.MortgageCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "房贷计算器")
@RestController
@RequestMapping("/mortgage")
@RequiredArgsConstructor
public class MortgageController {

    private final MortgageCalculatorService mortgageCalculatorService;

    @Operation(summary = "获取默认年利率（商贷/公积金）与说明")
    @GetMapping("/defaults")
    public ApiResponse<MortgageDefaultsResponse> getDefaults() {
        return ApiResponse.success(mortgageCalculatorService.getDefaults());
    }

    @Operation(summary = "计算月供、总利息与还款计划（支持商贷+公积金组合）")
    @PostMapping("/calculate")
    public ApiResponse<MortgageCalculateResponse> calculate(@Valid @RequestBody MortgageCalculateRequest request) {
        return ApiResponse.success(mortgageCalculatorService.calculate(request));
    }
}
