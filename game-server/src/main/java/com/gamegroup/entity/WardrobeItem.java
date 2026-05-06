package com.gamegroup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_wardrobe_item")
public class WardrobeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "image_file_name", length = 255)
    private String imageFileName;

    @Column(length = 100)
    private String name;

    @Column(name = "category_level1", length = 30)
    private String categoryLevel1;

    @Column(name = "category_level2", length = 30)
    private String categoryLevel2;

    @Column(length = 30)
    private String color;

    @Column(name = "pattern_tags", length = 255)
    private String patternTags;

    @Column(name = "season_tags", length = 100)
    private String seasonTags;

    @Column(name = "style_tags", length = 255)
    private String styleTags;

    @Column(name = "occasion_tags", length = 255)
    private String occasionTags;

    @Column(length = 50)
    private String material;

    @Column(name = "thickness_level")
    private Integer thicknessLevel;

    @Column(name = "warmth_level")
    private Integer warmthLevel;

    @Column(name = "waterproof_level")
    private Integer waterproofLevel;

    @Column(name = "fit_type", length = 30)
    private String fitType;

    @Column(name = "summary_text", length = 500)
    private String summaryText;

    @Column(name = "match_points", length = 500)
    private String matchPoints;

    @Column(name = "avoid_points", length = 500)
    private String avoidPoints;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "recognize_status", nullable = false, length = 20)
    private String recognizeStatus;

    @Column(name = "recognize_error_msg", length = 255)
    private String recognizeErrorMsg;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
        if (source == null) {
            source = "UPLOAD";
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (recognizeStatus == null) {
            recognizeStatus = "PENDING";
        }
    }
}
