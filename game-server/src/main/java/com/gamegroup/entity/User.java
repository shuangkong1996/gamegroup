package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String openid;

    @Column(length = 50, unique = true)
    private String username;

    @Column(length = 100)
    private String password;

    @Column(length = 64)
    private String unionId;

    @Column(length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String city;

    @Column(length = 16)
    private String gender;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "daily_style", length = 100)
    private String dailyStyle;

    @Column(name = "body_note", length = 500)
    private String bodyNote;

    @Column(name = "face_image_url", length = 512)
    private String faceImageUrl;

    @Column(name = "front_body_image_url", length = 512)
    private String frontBodyImageUrl;

    @Column(name = "side_body_image_url", length = 512)
    private String sideBodyImageUrl;

    @Column(name = "outfit_reference_image_url", length = 512)
    private String outfitReferenceImageUrl;

    @Column(name = "use_wardrobe_items")
    private Boolean useWardrobeItems;

    @Column(name = "profile_summary", length = 1000)
    private String profileSummary;

    @Column(name = "off_work_hour")
    private Integer offWorkHour;

    @Column(name = "off_work_minute")
    private Integer offWorkMinute;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
