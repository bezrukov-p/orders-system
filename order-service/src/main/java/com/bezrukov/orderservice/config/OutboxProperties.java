package com.bezrukov.orderservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {
    private int batchSize;
    private long fixedDelay;
}
