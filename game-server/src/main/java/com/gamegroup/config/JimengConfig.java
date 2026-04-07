package com.gamegroup.config;

import com.volcengine.ApiClient;
import com.volcengine.cv20240606.Cv20240606Api;
import com.volcengine.service.visual.impl.VisualServiceImpl;
import com.volcengine.sign.Credentials;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "volcengine.jimeng")
public class JimengConfig {

    private String accessKey;
    private String secretKey;
    private String endpoint = "visual.volcengineapi.com";
    private String modelName = "jimeng_i2v_first_v30_1080";

    @Bean
    public Cv20240606Api cv20240606Api() {
        ApiClient apiClient = new ApiClient()
                .setCredentials(Credentials.getCredentials(accessKey, secretKey))
                .setEndpoint("https://" + endpoint);
        
        log.info("Jimeng CV20240606 API initialized. Endpoint: {}, Model: {}", endpoint, modelName);
        return new Cv20240606Api(apiClient);
    }

    @Bean
    public VisualServiceImpl visualService() {
        VisualServiceImpl visualService = (VisualServiceImpl) VisualServiceImpl.getInstance();
        visualService.setAccessKey(accessKey);
        visualService.setSecretKey(secretKey);
        visualService.setHost(endpoint);
        visualService.setScheme("https");
        return visualService;
    }

    // Manual Getters
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
