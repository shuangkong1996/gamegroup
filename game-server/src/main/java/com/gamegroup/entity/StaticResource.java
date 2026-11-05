package com.gamegroup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "t_static_resource",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_static_resource_group_key",
                columnNames = {"group_key", "resource_key"}
        )
)
public class StaticResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_key", nullable = false, length = 64)
    private String groupKey;

    @Column(name = "resource_key", nullable = false, length = 64)
    private String resourceKey;

    @Column(name = "resource_name", nullable = false, length = 128)
    private String resourceName;

    @Column(name = "resource_url", nullable = false, length = 1024)
    private String resourceUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean status = true;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
