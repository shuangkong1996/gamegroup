package com.gamegroup.dto;

public record WechatJsSdkSignatureResponse(
        String appId,
        long timestamp,
        String nonceStr,
        String signature) {
}
