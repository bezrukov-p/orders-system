package com.bezrukov.orderservice.integrations;

import com.bezrukov.common.dto.OrderItemDto;
import com.bezrukov.common.event.ReserveStockCommand;
import com.bezrukov.orderservice.entity.OutboxMessage;
import com.bezrukov.orderservice.reposiroty.OutboxRepository;
import com.bezrukov.orderservice.service.impl.OutboxScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OutboxSchedulerIntegrationTest {
    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Test
    @DisplayName("Базовая отправка сообщений")
    void shouldPublishPendingMessagesToKafka() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        ReserveStockCommand command = ReserveStockCommand.builder()
                .orderId(orderId)
                .idempotencyKey(idempotencyKey.toString())
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(1L)
                                .quantity(2L)
                                .build()
                ))
                .build();

        String payload = objectMapper.writeValueAsString(command);

        OutboxMessage message = OutboxMessage.builder()
                .aggregateId(orderId)
                .eventType("ORDER_RESERVE_COMMAND")
                .payload(payload)
                .idempotencyKey(idempotencyKey.toString())
                .createdAt(LocalDateTime.now())
                .processed(false)
                .retryCount(0)
                .build();

        outboxRepository.save(message);

        outboxScheduler.publishPendingMessages();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OutboxMessage saved = outboxRepository.findById(message.getId()).orElseThrow();
                    assertThat(saved.isProcessed()).isTrue();
                    assertThat(saved.getProcessedAt()).isNotNull();
                });

        long pendingCount = outboxRepository.countByProcessedFalseAndFailedFalse();
        assertThat(pendingCount).isZero();
    }

    @Test
    @DisplayName("Обработка нескольких сообщений")
    void shouldProcessMultipleMessagesInBatch() throws Exception {
        for (int i = 0; i < 5; i++) {
            UUID orderId = UUID.randomUUID();
            ReserveStockCommand command = ReserveStockCommand.builder()
                    .orderId(orderId)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .items(List.of(
                            OrderItemDto.builder()
                                    .productId((long) i)
                                    .quantity(1L)
                                    .build()
                    ))
                    .build();

            String payload = objectMapper.writeValueAsString(command);

            OutboxMessage message = OutboxMessage.builder()
                    .aggregateId(orderId)
                    .eventType("ORDER_RESERVE_COMMAND")
                    .payload(payload)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .createdAt(LocalDateTime.now())
                    .processed(false)
                    .retryCount(0)
                    .build();

            outboxRepository.save(message);
        }

        outboxScheduler.publishPendingMessages();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long pendingCount = outboxRepository.countByProcessedFalseAndFailedFalse();
                    assertThat(pendingCount).isZero();
                });

        List<OutboxMessage> allMessages = outboxRepository.findAll();
        assertThat(allMessages)
                .allMatch(OutboxMessage::isProcessed)
                .allMatch(msg -> msg.getProcessedAt() != null);
    }

    @Test
    @DisplayName("Игнорирование уже обработанных сообщений")
    void shouldIgnoreAlreadyProcessedMessages(){
        OutboxMessage processedMessage = OutboxMessage.builder()
                .aggregateId(UUID.randomUUID())
                .eventType("ORDER_RESERVE_COMMAND")
                .payload("{}")
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .processed(true)
                .processedAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        outboxRepository.save(processedMessage);

        outboxScheduler.publishPendingMessages();

        await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OutboxMessage saved = outboxRepository.findById(processedMessage.getId()).orElseThrow();
                    assertThat(saved.isProcessed()).isTrue();
                    assertThat(saved.getRetryCount()).isZero();
                });
    }

    @Test
    @DisplayName("Порядок обработки событий")
    void shouldProcessMessagesInFifoOrder() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            UUID orderId = UUID.randomUUID();
            ReserveStockCommand command = ReserveStockCommand.builder()
                    .orderId(orderId)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .items(List.of(
                            OrderItemDto.builder()
                                    .productId((long) i)
                                    .quantity(1L)
                                    .build()
                    ))
                    .build();

            String payload = objectMapper.writeValueAsString(command);

            OutboxMessage message = OutboxMessage.builder()
                    .aggregateId(orderId)
                    .eventType("ORDER_RESERVE_COMMAND")
                    .payload(payload)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .createdAt(now.minusMinutes(i))
                    .processed(false)
                    .retryCount(0)
                    .build();

            outboxRepository.save(message);
        }

        outboxScheduler.publishPendingMessages();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long pendingCount = outboxRepository.countByProcessedFalseAndFailedFalse();
                    assertThat(pendingCount).isZero();
                });
    }
}