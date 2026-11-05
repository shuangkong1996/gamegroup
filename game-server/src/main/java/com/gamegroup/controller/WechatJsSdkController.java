package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.WechatJsSdkSignatureResponse;
import com.gamegroup.service.WechatJsSdkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/wechat/js-sdk")
public class WechatJsSdkController {

    private final WechatJsSdkService wechatJsSdkService;

    public WechatJsSdkController(WechatJsSdkService wechatJsSdkService) {
        this.wechatJsSdkService = wechatJsSdkService;
    }

    @GetMapping("/signature")
    public ApiResponse<WechatJsSdkSignatureResponse> getSignature(
            @RequestParam String url) {
        return ApiResponse.success(wechatJsSdkService.createSignature(url));
    }
}
