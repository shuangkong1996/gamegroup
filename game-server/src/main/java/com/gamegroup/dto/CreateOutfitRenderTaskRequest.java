package com.gamegroup.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOutfitRenderTaskRequest {

    @Size(max = 30, message = "模式信息过长")
    private String mode;

    @Size(max = 50, message = "城市信息过长")
    private String city;

    @Size(max = 120, message = "场景信息过长")
    private String sceneRequirement;

    private Boolean useWardrobeItems;

    @Size(max = 20, message = "目标日期格式错误")
    private String targetDate;
}
