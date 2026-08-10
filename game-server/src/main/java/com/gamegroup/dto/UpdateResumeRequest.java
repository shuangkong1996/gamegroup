package com.gamegroup.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateResumeRequest {

    @NotBlank(message = "简历标题不能为空")
    @Size(max = 100, message = "简历标题最多 100 字")
    private String title;

    @NotBlank(message = "模板 ID 不能为空")
    @Size(max = 50, message = "模板 ID 最多 50 字")
    private String templateId;

    @NotNull(message = "简历内容不能为空")
    private JsonNode content;
}
