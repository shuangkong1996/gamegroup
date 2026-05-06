package com.gamegroup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "t_community")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 40)
    private String city;

    @Column(length = 40)
    private String district;

    @Column(name = "cover_image", length = 512)
    private String coverImage;

    @Column(name = "listing_avg_price", precision = 12, scale = 2)
    private BigDecimal listingAvgPrice;

    @Column(name = "price_change_pct_1y", precision = 8, scale = 2)
    private BigDecimal priceChangePct1y;

    @Column(name = "showing_count_recent")
    private Integer showingCountRecent;

    @Column(name = "onsale_count")
    private Integer onsaleCount;

    @Column(name = "built_year")
    private Integer builtYear;

    @Column(length = 100)
    private String developer;

    @Column(name = "property_company", length = 100)
    private String propertyCompany;

    @Column(name = "building_type", length = 50)
    private String buildingType;

    @Column(name = "plot_ratio", precision = 8, scale = 2)
    private BigDecimal plotRatio;

    @Column(name = "greening_rate", precision = 8, scale = 2)
    private BigDecimal greeningRate;

    @Column(name = "parking_ratio", precision = 8, scale = 2)
    private BigDecimal parkingRatio;

    @Column(name = "property_fee", precision = 10, scale = 2)
    private BigDecimal propertyFee;

    @Column(name = "subway_distance_meters")
    private Integer subwayDistanceMeters;

    @Column(name = "walk_to_rail_minutes")
    private Integer walkToRailMinutes;

    @Column(name = "primary_school", length = 100)
    private String primarySchool;

    @Column(name = "middle_school", length = 100)
    private String middleSchool;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (updateTime == null) {
            updateTime = LocalDateTime.now();
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
