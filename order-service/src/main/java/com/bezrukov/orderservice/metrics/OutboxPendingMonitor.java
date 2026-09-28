package com.bezrukov.orderservice.metrics;

import com.bezrukov.orderservice.reposiroty.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPendingMonitor {

    private final OutboxRepository outboxRepository;
    private final OutboxMetrics metrics;

    @Scheduled(fixedRate = 2000)
    public void updatePendingMetric() {
        long pending = outboxRepository.countByProcessedFalseAndFailedFalse();
        metrics.setPending(pending);
    }
}

