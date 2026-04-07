package com.gamegroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLedgerAccountRequest {

    @NotBlank(message = "账户名称不能为空")
    private String name;

    private String type;

    private String icon;
}
