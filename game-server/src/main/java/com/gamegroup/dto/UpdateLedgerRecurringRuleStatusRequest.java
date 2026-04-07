package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateLedgerRecurringRuleStatusRequest {

    @NotBlank(message = "status不能为空")
    @Pattern(regexp = "ACTIVE|PAUSED", message = "status仅支持ACTIVE或PAUSED")
    private String status;
}

