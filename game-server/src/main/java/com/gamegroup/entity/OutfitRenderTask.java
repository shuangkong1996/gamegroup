package com.gamegroup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "t_outfit_render_task")
public class OutfitRenderTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String mode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "weather_city", length = 50)
    private String weatherCity;

    @Column(name = "weather_condition", length = 50)
    private String weatherCondition;

    @Column(name = "temperature_min")
    private Integer temperatureMin;

    @Column(name = "temperature_max")
    private Integer temperatureMax;

    @Column
    private Integer humidity;

    @Column(name = "scene_requirement", length = 120)
    private String sceneRequirement;

    @Column(name = "use_wardrobe_items")
    private Boolean useWardrobeItems;

    @Column(name = "plan_title", length = 120)
    private String planTitle;

    @Column(name = "plan_summary", columnDefinition = "TEXT")
    private String planSummary;

    @Column(name = "plan_reason", columnDefinition = "TEXT")
    private String planReason;

    @Column(name = "comfort_tip", length = 255)
    private String comfortTip;

    @Column(name = "style_keywords", length = 255)
    private String styleKeywords;

    @Column(name = "scene_keywords", length = 255)
    private String sceneKeywords;

    @Column(name = "selected_item_ids_json", columnDefinition = "TEXT")
    private String selectedItemIdsJson;

    @Column(name = "plan_json", columnDefinition = "MEDIUMTEXT")
    private String planJson;

    @Column(name = "render_prompt", columnDefinition = "TEXT")
    private String renderPrompt;

    @Column(name = "result_image_url", length = 512)
    private String resultImageUrl;

    @Column(name = "provider_task_id", length = 128)
    private String providerTaskId;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

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
        if (status == null) {
            status = "PENDING";
        }
        if (targetDate == null) {
            targetDate = LocalDate.now().plusDays(1);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
