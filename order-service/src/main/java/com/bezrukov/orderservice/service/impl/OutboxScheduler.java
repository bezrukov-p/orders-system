package com.bezrukov.orderservice.service.impl;

import com.bezrukov.common.event.ReserveStockCommand;
import com.bezrukov.orderservice.config.OutboxProperties;
import com.bezrukov.orderservice.entity.OutboxMessage;
import com.bezrukov.orderservice.kafka.OrderCommandProducer;
import com.bezrukov.orderservice.metrics.OutboxMetrics;
import com.bezrukov.orderservice.reposiroty.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final OrderCommandProducer orderCommandProducer;
    private final ObjectMapper objectMapper;
    private final OutboxProperties outboxProperties;

    private final OutboxMetrics metrics;

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay}")
    @Transactional
    @WithSpan("publish.messages.sheduler")
    public void publishPendingMessages() {
        long startTime = System.currentTimeMillis();

        long pendingBefore = outboxRepository.countByProcessedFalseAndFailedFalse();

        if (pendingBefore == 0) {
            log.debug("Outbox is empty, nothing to process");
            return;
        }
        if (pendingBefore > outboxProperties.getBatchSize()) {
            log.warn("More unprocessed orders than batch size");
        }

        log.info("Outbox processing started: pendingBefore={}", pendingBefore);

        List<OutboxMessage> messages = outboxRepository.findByProcessedFalseAndFailedFalseOrderByCreatedAtAsc(
                Limit.of(outboxProperties.getBatchSize())
        );
        if (messages.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox messages", messages.size());

        long successCount = 0;
        long failureCount = 0;
        for (OutboxMessage message : messages) {
            try {
                ReserveStockCommand event = switch (message.getEventType()) {
                    case "ORDER_RESERVE_COMMAND" -> objectMapper.readValue( //TODO вынести в константу
                            message.getPayload(), ReserveStockCommand.class
                    );
                    default -> throw new IllegalStateException("Unknown event type: " + message.getEventType());
                };

                orderCommandProducer.sendReserveStockCommand(event);

                message.setProcessed(true);
                message.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(message);

                successCount++;
                log.info("Outbox message published: id={}, type={}", message.getId(), message.getEventType());

            } catch (Exception e) {
                if (message.getRetryCount() > outboxProperties.getMaxRetries()) {
                    message.setFailed(true);
                    log.error("Outbox message PERMANENTLY FAILED: id={}, type={}, retries={}, error={}",
                            message.getId(), message.getEventType(), message.getRetryCount() + 1, e.getMessage(), e);
                    continue;
                }

                message.setRetryCount(message.getRetryCount() + 1);
                outboxRepository.save(message);
                failureCount++;

                log.error("Failed to publish outbox message: id={}, retry={}, error={}",
                        message.getId(), message.getRetryCount(), e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        metrics.recordBatchDuration(Duration.ofMillis(duration));

        log.info("Outbox batch completed: " +
                        "processed={}, failed={}, pendingBefore={}, " +
                        "batchSize={}, duration={}ms, rate={}msg/sec",
                successCount, failureCount, pendingBefore,
                messages.size(), duration,
                duration > 0 ? (successCount / (duration * 1000)) : 0
        );
    }
}
