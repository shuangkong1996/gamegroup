package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.*;
import com.gamegroup.entity.User;
import com.gamegroup.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "微信登录")
    @PostMapping("/wx-login")
    public ApiResponse<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        LoginResponse response = authService.wxLogin(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/admin/login")
    public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        LoginResponse response = authService.adminLogin(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "管理员注册")
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public ApiResponse<User> getUserInfo(@RequestAttribute("userId") Long userId) {
        User user = authService.getUserById(userId);
        return ApiResponse.success(user);
    }

    @Operation(summary = "获取用户设置")
    @GetMapping("/settings")
    public ApiResponse<UserSettingsDTO> getUserSettings(@RequestAttribute("userId") Long userId) {
        UserSettingsDTO settings = authService.getUserSettings(userId);
        return ApiResponse.success(settings);
    }

    @Operation(summary = "更新用户设置")
    @PutMapping("/settings")
    public ApiResponse<UserSettingsDTO> updateUserSettings(
            @Valid @RequestBody UpdateUserSettingsRequest request,
            @RequestAttribute("userId") Long userId) {
        UserSettingsDTO settings = authService.updateUserSettings(userId, request);
        return ApiResponse.success(settings);
    }

    @Operation(summary = "获取用户形象资料")
    @GetMapping("/profile")
    public ApiResponse<UserProfileDTO> getUserProfile(@RequestAttribute("userId") Long userId) {
        UserProfileDTO profile = authService.getUserProfile(userId);
        return ApiResponse.success(profile);
    }

    @Operation(summary = "更新用户形象资料")
    @PutMapping("/profile")
    public ApiResponse<UserProfileDTO> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request,
            @RequestAttribute("userId") Long userId) {
        UserProfileDTO profile = authService.updateUserProfile(userId, request);
        return ApiResponse.success(profile);
    }
}
