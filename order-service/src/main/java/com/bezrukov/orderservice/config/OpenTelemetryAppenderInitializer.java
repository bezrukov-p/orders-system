package com.bezrukov.orderservice.config;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

@Component
public class OpenTelemetryAppenderInitializer implements InitializingBean {
    private final OpenTelemetry openTelemetry;

    public OpenTelemetryAppenderInitializer(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }
}
