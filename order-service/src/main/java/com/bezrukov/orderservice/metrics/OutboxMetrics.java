package com.bezrukov.orderservice.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class OutboxMetrics {

    private final MeterRegistry registry;
    private final AtomicLong pendingValue = new AtomicLong(0);

    private Timer batchTimer;

    @PostConstruct
    void init() {
        Gauge.builder("outbox.pending", pendingValue, AtomicLong::get)
                .description("Number of unprocessed outbox messages in DB")
                .register(registry);
        this.batchTimer = Timer.builder("outbox.batch.duration")
                .description("Duration of outbox batch processing")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void setPending(long value) {
        pendingValue.set(value);
    }

    public void recordBatchDuration(Duration duration) {
        batchTimer.record(duration);
    }
}
