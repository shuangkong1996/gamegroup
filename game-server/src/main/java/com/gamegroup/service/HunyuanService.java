package com.gamegroup.service;

import com.gamegroup.dto.AnimeGenerateRequest;
import com.gamegroup.dto.AnimeGenerateResponse;
import com.tencentcloudapi.aiart.v20221229.AiartClient;
import com.tencentcloudapi.aiart.v20221229.models.ImageToImageRequest;
import com.tencentcloudapi.aiart.v20221229.models.ImageToImageResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class HunyuanService {

    private final MinioService minioService;

    @Value("${tencent.secretId:}")
    private String secretId;

    @Value("${tencent.secretKey:}")
    private String secretKey;

    @Value("${tencent.region:ap-shanghai}")
    private String region;

    public AnimeGenerateResponse generateAnimeImage(AnimeGenerateRequest request) {
        if (secretId == null || secretId.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new RuntimeException("服务端未配置腾讯云密钥");
        }

        try {
            // 1. 下载图片并转 Base64
            InputStream inputStream = minioService.getStreamFromUrl(request.getImageUrl());
            byte[] imageBytes = StreamUtils.copyToByteArray(inputStream);
            String inputBase64 = Base64.getEncoder().encodeToString(imageBytes);

            // 2. 调用腾讯云 API
            Credential cred = new Credential(secretId, secretKey);
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("aiart.tencentcloudapi.com");
            // 增加超时时间，防止生成慢导致断开
            httpProfile.setReadTimeout(60); 

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            AiartClient client = new AiartClient(cred, region, clientProfile);

            ImageToImageRequest req = new ImageToImageRequest();
            req.setInputImage(inputBase64);
            // 优化 Prompt: 强调卡通、Q版、可爱特征，去除"唯美"防止过于写实
            req.setPrompt("卡通, Q版, 可爱, 二次元, 动漫风格, 线条清晰, 活泼, 鲜艳, 3D渲染风格");
            // 显式指定风格 ID: 201 (日系动漫)
            req.setStyles(new String[]{"201"});
            // 提高强度: 让 AI 有更多发挥空间，使其更像卡通而非真人滤镜
            req.setStrength(0.75f); 

            ImageToImageResponse resp = client.ImageToImage(req);

            // 3. 处理结果：Base64 -> MinIO
            String resultBase64 = resp.getResultImage();
            byte[] resultBytes = Base64.getDecoder().decode(resultBase64);
            ByteArrayInputStream resultStream = new ByteArrayInputStream(resultBytes);
            
            String fileName = minioService.uploadStream(resultStream, "image/jpeg");
            String resultUrl = minioService.getFileUrl(fileName);

            return new AnimeGenerateResponse(resultUrl, resp.getRequestId());

        } catch (TencentCloudSDKException e) {
            log.error("Tencent Cloud API Error: {}", e.toString());
            throw new RuntimeException("生成失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected Error: ", e);
            throw new RuntimeException("服务内部错误: " + e.getMessage());
        }
    }
}
