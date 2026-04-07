package com.gamegroup.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
