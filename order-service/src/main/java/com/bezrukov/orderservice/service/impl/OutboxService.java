package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.entity.OutboxMessage;
import com.bezrukov.orderservice.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(UUID aggregateId, String eventType, Object event, String idempotencyKey) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxMessage message = OutboxMessage.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payload)
                    .idempotencyKey(idempotencyKey)
                    .createdAt(LocalDateTime.now())
                    .processed(false)
                    .retryCount(0)
                    .build();

            outboxRepository.save(message);
            log.debug("Outbox message saved: type={}, aggregateId={}", eventType, aggregateId);

        } catch (Exception e) {
            log.error("Failed to serialize event for outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save outbox message", e);
        }
    }
}
