package com.gamegroup.config;

import com.gamegroup.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/wx-login",
                        "/auth/admin/login",
                        "/auth/register", // 如果需要开放注册
                        "/error",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/articles", // 允许未登录查看文章列表
                        "/articles/*", // 允许未登录查看文章详情
                        "/static-resources/**", // 静态资源地址供未登录页面读取
                        "/v1/wedding/invitations/*/rsvps", // 婚礼宾客无需登录即可回复
                        "/v1/wechat/js-sdk/signature", // 婚礼页面获取微信分享签名
                        "/mortgage/**", // 房贷计算器（纯计算，无需登录）
                        "/community/**", // 小区对比助手（公共工具）
                        "/resume/pdf" // 简历 PDF（内容由客户端提交，无需登录）
                );
    }
}
