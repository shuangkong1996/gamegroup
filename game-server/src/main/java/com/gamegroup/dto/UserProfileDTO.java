package com.gamegroup.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private String gender;

    private Integer heightCm;

    private BigDecimal weightKg;

    private String dailyStyle;

    private String bodyNote;

    private String faceImageUrl;

    private String frontBodyImageUrl;

    private String sideBodyImageUrl;

    private String outfitReferenceImageUrl;

    private Boolean useWardrobeItems;

    private String profileSummary;

    private Boolean basicInfoCompleted;

    private Boolean imageInfoCompleted;

    private Boolean profileCompleted;
}
