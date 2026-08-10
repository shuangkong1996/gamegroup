package com.gamegroup.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDTO {

    private Long id;

    private String title;

    private String templateId;

    private JsonNode content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
