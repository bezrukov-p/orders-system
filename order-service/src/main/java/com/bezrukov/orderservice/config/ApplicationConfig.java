package com.bezrukov.orderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableScheduling
@EnableWebSecurity
@EnableConfigurationProperties(OutboxProperties.class)
public class ApplicationConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
