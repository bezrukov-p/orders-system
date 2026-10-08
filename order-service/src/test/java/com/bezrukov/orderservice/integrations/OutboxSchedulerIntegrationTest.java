package com.bezrukov.orderservice.integrations;

import com.bezrukov.common.dto.OrderItemDto;
import com.bezrukov.common.event.ReserveStockCommand;
import com.bezrukov.orderservice.entity.OutboxMessage;
import com.bezrukov.orderservice.repository.OutboxRepository;
import com.bezrukov.orderservice.service.impl.OutboxScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OutboxSchedulerIntegrationTest {

    private static final String EVENT_TYPE_RESERVE_STOCK = "ORDER_RESERVE_COMMAND";
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_POLL = Duration.ofMillis(100);

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("Базовая отправка сообщений")
    void shouldPublishPendingMessagesToKafka() throws Exception {
        OutboxMessage message = savePendingOutboxMessage(1L, 2L, LocalDateTime.now());

        outboxScheduler.publishPendingMessages();

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .ignoreException(NoSuchElementException.class)
                .untilAsserted(() -> {
                    OutboxMessage saved = outboxRepository.findById(message.getId()).orElseThrow();
                    assertThat(saved.isProcessed()).isTrue();
                    assertThat(saved.getProcessedAt()).isNotNull();
                });

        assertThat(outboxRepository.countByProcessedFalseAndFailedFalse()).isZero();
    }

    @Test
    @DisplayName("Обработка нескольких сообщений")
    void shouldProcessMultipleMessagesInBatch() throws Exception {
        int messagesCount = 5;
        for (int i = 0; i < messagesCount; i++) {
            savePendingOutboxMessage((long) i, 1L, LocalDateTime.now());
        }

        outboxScheduler.publishPendingMessages();

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .untilAsserted(() ->
                        assertThat(outboxRepository.countByProcessedFalseAndFailedFalse()).isZero()
                );

        List<OutboxMessage> allMessages = outboxRepository.findAll();
        assertThat(allMessages)
                .hasSize(messagesCount)
                .allMatch(OutboxMessage::isProcessed)
                .allMatch(msg -> msg.getProcessedAt() != null);
    }

    @Test
    @DisplayName("Игнорирование уже обработанных сообщений")
    void shouldIgnoreAlreadyProcessedMessages(){
        OutboxMessage processedMessage = outboxRepository.save(OutboxMessage.builder()
                .aggregateId(UUID.randomUUID())
                .eventType(EVENT_TYPE_RESERVE_STOCK)
                .payload("{}")
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .processed(true)
                .processedAt(LocalDateTime.now())
                .retryCount(0)
                .build());

        outboxScheduler.publishPendingMessages();

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .ignoreException(NoSuchElementException.class)
                .untilAsserted(() -> {
                    OutboxMessage saved = outboxRepository.findById(processedMessage.getId()).orElseThrow();
                    assertThat(saved.isProcessed()).isTrue();
                    assertThat(saved.getRetryCount()).isZero();
                });
    }

    private OutboxMessage savePendingOutboxMessage(long productId, long quantity, LocalDateTime createdAt)
            throws JsonProcessingException {

        UUID orderId = UUID.randomUUID();

        ReserveStockCommand command = ReserveStockCommand.builder()
                .orderId(orderId)
                .idempotencyKey(UUID.randomUUID().toString())
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(productId)
                                .quantity(quantity)
                                .build()
                ))
                .build();

        OutboxMessage message = OutboxMessage.builder()
                .aggregateId(orderId)
                .eventType(EVENT_TYPE_RESERVE_STOCK)
                .payload(objectMapper.writeValueAsString(command))
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(createdAt)
                .processed(false)
                .retryCount(0)
                .build();

        return outboxRepository.save(message);
    }
}