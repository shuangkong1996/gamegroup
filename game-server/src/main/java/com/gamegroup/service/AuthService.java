package com.gamegroup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.common.util.JwtUtil;
import com.gamegroup.config.WxConfig;
import com.gamegroup.dto.*;
import com.gamegroup.entity.User;
import com.gamegroup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WxConfig wxConfig;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request) {
        String openid = getOpenidFromWx(request.getCode());

        Optional<User> existingUser = userRepository.findByOpenid(openid);
        boolean isNewUser = existingUser.isEmpty();

        User user;
        if (isNewUser) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(request.getNickname());
            user.setAvatarUrl(request.getAvatarUrl());
            user = userRepository.save(user);
            log.info("New user registered: userId={}, openid={}", user.getId(), openid);
        } else {
            user = existingUser.get();
            if (request.getNickname() != null) {
                user.setNickname(request.getNickname());
            }
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(isNewUser)
                .build();
    }

    @Transactional
    public LoginResponse adminLogin(AdminLoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 简单的 MD5 加密验证 (实际项目应使用 BCrypt)
        String inputPassword = DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!inputPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(false)
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // 简单的 MD5 加密
        user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setNickname(request.getUsername());
        // 生成一个虚拟的 openid 以满足非空约束
        user.setOpenid("WEB_ADMIN_" + UUID.randomUUID().toString());
        
        userRepository.save(user);
        log.info("New admin registered: {}", request.getUsername());
    }

    private String getOpenidFromWx(String code) {
        try {
            WebClient webClient = webClientBuilder.build();

            // 微信API返回Content-Type是text/plain，需要先读取为String再解析JSON
            String responseBody = webClient.get()
                    .uri(WX_LOGIN_URL, uriBuilder -> uriBuilder
                            .queryParam("appid", wxConfig.getAppid())
                            .queryParam("secret", wxConfig.getSecret())
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isEmpty()) {
                log.error("WeChat login failed: empty response");
                throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
            }

            // 手动解析JSON
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            if (response.containsKey("errcode")) {
                log.error("WeChat login failed: {}", response);
                throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
            }

            return (String) response.get("openid");
        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse WeChat API response: {}", e.getMessage());
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
        } catch (Exception e) {
            log.error("WeChat API call failed", e);
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
        }
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * 获取用户设置
     */
    public UserSettingsDTO getUserSettings(Long userId) {
        User user = getUserById(userId);
        return UserSettingsDTO.builder()
                .offWorkHour(user.getOffWorkHour() != null ? user.getOffWorkHour() : 18)
                .offWorkMinute(user.getOffWorkMinute() != null ? user.getOffWorkMinute() : 0)
                .city(user.getCity())
                .build();
    }

    /**
     * 更新用户设置
     */
    @Transactional
    public UserSettingsDTO updateUserSettings(Long userId, UpdateUserSettingsRequest request) {
        User user = getUserById(userId);
        
        if (request.getOffWorkHour() != null) {
            user.setOffWorkHour(request.getOffWorkHour());
        }
        if (request.getOffWorkMinute() != null) {
            user.setOffWorkMinute(request.getOffWorkMinute());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity().trim());
        }
        
        user = userRepository.save(user);
        log.info("User settings updated: userId={}, offWorkHour={}, offWorkMinute={}, city={}", 
                userId, user.getOffWorkHour(), user.getOffWorkMinute(), user.getCity());
        
        return UserSettingsDTO.builder()
                .offWorkHour(user.getOffWorkHour() != null ? user.getOffWorkHour() : 18)
                .offWorkMinute(user.getOffWorkMinute() != null ? user.getOffWorkMinute() : 0)
                .city(user.getCity())
                .build();
    }

    public UserProfileDTO getUserProfile(Long userId) {
        return toUserProfileDTO(getUserById(userId));
    }

    @Transactional
    public UserProfileDTO updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getUserById(userId);

        user.setGender(normalize(request.getGender()));
        user.setHeightCm(request.getHeightCm());
        user.setWeightKg(request.getWeightKg());
        user.setDailyStyle(normalize(request.getDailyStyle()));
        user.setBodyNote(normalize(request.getBodyNote()));
        user.setFaceImageUrl(normalize(request.getFaceImageUrl()));
        user.setFrontBodyImageUrl(normalize(request.getFrontBodyImageUrl()));
        user.setSideBodyImageUrl(normalize(request.getSideBodyImageUrl()));
        user.setOutfitReferenceImageUrl(normalize(request.getOutfitReferenceImageUrl()));
        user.setUseWardrobeItems(request.getUseWardrobeItems() == null ? Boolean.TRUE : request.getUseWardrobeItems());

        user = userRepository.save(user);
        log.info("User profile updated: userId={}", userId);
        return toUserProfileDTO(user);
    }

    private UserProfileDTO toUserProfileDTO(User user) {
        boolean basicInfoCompleted = user.getHeightCm() != null
                && user.getWeightKg() != null
                && hasText(user.getGender())
                && hasText(user.getDailyStyle());
        boolean imageInfoCompleted = hasText(user.getFaceImageUrl())
                && hasText(user.getFrontBodyImageUrl())
                && hasText(user.getSideBodyImageUrl());

        return UserProfileDTO.builder()
                .gender(user.getGender())
                .heightCm(user.getHeightCm())
                .weightKg(user.getWeightKg())
                .dailyStyle(user.getDailyStyle())
                .bodyNote(user.getBodyNote())
                .faceImageUrl(user.getFaceImageUrl())
                .frontBodyImageUrl(user.getFrontBodyImageUrl())
                .sideBodyImageUrl(user.getSideBodyImageUrl())
                .outfitReferenceImageUrl(user.getOutfitReferenceImageUrl())
                .useWardrobeItems(user.getUseWardrobeItems() == null ? Boolean.TRUE : user.getUseWardrobeItems())
                .profileSummary(user.getProfileSummary())
                .basicInfoCompleted(basicInfoCompleted)
                .imageInfoCompleted(imageInfoCompleted)
                .profileCompleted(basicInfoCompleted && imageInfoCompleted)
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
