package com.gamegroup;

import com.gamegroup.config.MortgageDefaultsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(MortgageDefaultsProperties.class)
public class GameGroupApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameGroupApplication.class, args);
    }
}
