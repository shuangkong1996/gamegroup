package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.dto.WechatJsSdkSignatureResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class WechatJsSdkService {

    private static final Logger log = LoggerFactory.getLogger(WechatJsSdkService.class);
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String JSAPI_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket";
    private static final long CACHE_SAFETY_WINDOW_MILLIS = 5 * 60 * 1000L;

    private final String appId;
    private final String appSecret;
    private final String allowedDomain;
    private final RestTemplate restTemplate = new RestTemplate();

    private String accessToken;
    private long accessTokenExpiresAt;
    private String jsapiTicket;
    private long jsapiTicketExpiresAt;

    public WechatJsSdkService(
            @Value("${wx.official-account.appid:wx32238ffba13a6470}") String appId,
            @Value("${wx.official-account.secret:}") String appSecret,
            @Value("${wx.official-account.js-sdk-domain:wedding.shuangkong.top}") String allowedDomain) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.allowedDomain = allowedDomain;
    }

    public WechatJsSdkSignatureResponse createSignature(String pageUrl) {
        validatePageUrl(pageUrl);
        if (appId.isBlank() || appSecret.isBlank()) {
            throw new BusinessException(500, "微信公众号分享配置未完成");
        }

        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis() / 1000;
        String source = "jsapi_ticket=" + getJsapiTicket()
                + "&noncestr=" + nonceStr
                + "&timestamp=" + timestamp
                + "&url=" + pageUrl;

        return new WechatJsSdkSignatureResponse(appId, timestamp, nonceStr, sha1(source));
    }

    private void validatePageUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.isBlank() || pageUrl.length() > 2048) {
            throw new BusinessException(400, "页面地址格式错误");
        }
        try {
            URI uri = URI.create(pageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !allowedDomain.equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawFragment() != null) {
                throw new BusinessException(400, "仅支持婚礼请帖域名获取分享签名");
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "页面地址格式错误");
        }
    }

    private synchronized String getJsapiTicket() {
        long now = System.currentTimeMillis();
        if (jsapiTicket != null && jsapiTicketExpiresAt - CACHE_SAFETY_WINDOW_MILLIS > now) {
            return jsapiTicket;
        }

        String token = getAccessToken(now);

        Map<?, ?> ticketResponse = getWechatResponse(UriComponentsBuilder.fromUriString(JSAPI_TICKET_URL)
                .queryParam("access_token", token)
                .queryParam("type", "jsapi")
                .build()
                .encode()
                .toUri());
        String ticket = requireString(ticketResponse, "ticket");
        long ticketLifetime = requireLifetime(ticketResponse);
        jsapiTicket = ticket;
        jsapiTicketExpiresAt = now + ticketLifetime * 1000;
        return jsapiTicket;
    }

    private String getAccessToken(long now) {
        if (accessToken != null && accessTokenExpiresAt - CACHE_SAFETY_WINDOW_MILLIS > now) {
            return accessToken;
        }

        Map<?, ?> tokenResponse = getWechatResponse(UriComponentsBuilder.fromUriString(ACCESS_TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .build()
                .encode()
                .toUri());
        accessToken = requireString(tokenResponse, "access_token");
        accessTokenExpiresAt = now + requireLifetime(tokenResponse) * 1000;
        return accessToken;
    }

    private Map<?, ?> getWechatResponse(URI uri) {
        try {
            Map<?, ?> response = restTemplate.getForObject(uri, Map.class);
            Object errorCode = response == null ? null : response.get("errcode");
            if (response == null || (errorCode instanceof Number number && number.intValue() != 0)) {
                log.warn("WeChat JS-SDK credential request failed: {}", response);
                throw new BusinessException(502, "获取微信分享凭据失败");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("WeChat JS-SDK credential request failed", e);
            throw new BusinessException(502, "获取微信分享凭据失败");
        }
    }

    private String requireString(Map<?, ?> response, String key) {
        Object value = response.get(key);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        throw new BusinessException(502, "微信分享凭据响应无效");
    }

    private long requireLifetime(Map<?, ?> response) {
        Object value = response.get("expires_in");
        if (value instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        throw new BusinessException(502, "微信分享凭据有效期无效");
    }

    private String sha1(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is unavailable", e);
        }
    }
}
