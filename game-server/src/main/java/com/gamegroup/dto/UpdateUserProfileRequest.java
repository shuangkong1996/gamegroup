package com.gamegroup.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @Size(max = 16, message = "性别信息过长")
    private String gender;

    @Min(value = 80, message = "身高需在80到250厘米之间")
    @Max(value = 250, message = "身高需在80到250厘米之间")
    private Integer heightCm;

    @DecimalMin(value = "20.0", message = "体重需在20到300千克之间")
    @DecimalMax(value = "300.0", message = "体重需在20到300千克之间")
    private BigDecimal weightKg;

    @Size(max = 100, message = "日常风格信息过长")
    private String dailyStyle;

    @Size(max = 500, message = "备注信息过长")
    private String bodyNote;

    @Size(max = 512, message = "图片地址过长")
    private String faceImageUrl;

    @Size(max = 512, message = "图片地址过长")
    private String frontBodyImageUrl;

    @Size(max = 512, message = "图片地址过长")
    private String sideBodyImageUrl;

    @Size(max = 512, message = "图片地址过长")
    private String outfitReferenceImageUrl;

    private Boolean useWardrobeItems;
}
