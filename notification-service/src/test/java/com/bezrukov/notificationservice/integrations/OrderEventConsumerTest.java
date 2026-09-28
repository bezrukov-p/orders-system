package com.bezrukov.notificationservice.integrations;

import com.bezrukov.common.event.OrderConfirmedEvent;
import com.bezrukov.common.event.OrderItemEvent;
import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderEventConsumerTest {

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private static final Duration KAFKA_SEND_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_POLL = Duration.ofMillis(100);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("При получении OrderConfirmedEvent заказ сохраняется")
    void shouldSaveOrderToDatabase() {
        UUID orderId = UUID.randomUUID();
        double totalPrice = 1999.98;
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .userEmail("test@example.com")
                .totalPrice(totalPrice)
                .items(List.of(
                        item(1L, 2L, 999.99, 0)
                ))
                .createdAt(LocalDateTime.now())
                .build();

        sendOrderConfirmedEvent(orderId, event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .ignoreException(NoSuchElementException.class)
                .untilAsserted(() -> {
                    Order saved = orderOf(orderId);
                    assertThat(saved.getOrderId()).isEqualTo(orderId);
                    assertThat(saved.getTotalPrice()).isEqualTo(totalPrice);
                });
    }

    @Test
    @DisplayName("Повторное событие не создает дубликат")
    void shouldIgnoreDuplicateEvent() {
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .totalPrice(1999.98)
                .items(List.of())
                .build();

        sendOrderConfirmedEvent(orderId, event);
        sendOrderConfirmedEvent(orderId, event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .untilAsserted(() -> {
                    long count = orderRepository.countByOrderId(orderId);
                    assertThat(count).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Заказ сохраняется с позициями")
    void shouldSaveOrderWithItems() {
        UUID orderId = UUID.randomUUID();
        double totalPrice = 2044.97;
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .totalPrice(totalPrice)
                .items(List.of(
                        item(1L, 2L, 999.99, 0),
                        item(2L, 1L, 49.99, 10)
                ))
                .build();

        sendOrderConfirmedEvent(orderId, event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .untilAsserted(() -> {
                    Order saved = orderWithItemsOf(orderId);
                    assertThat(saved.getItems()).hasSize(2);
                    assertThat(saved.getTotalPrice()).isEqualTo(totalPrice);
                });
    }

    private OrderItemEvent item(long productId, long quantity, double price, int salePercent) {
        return OrderItemEvent.builder()
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .salePercent(salePercent)
                .build();
    }

    private void sendOrderConfirmedEvent(UUID orderId, OrderConfirmedEvent event) {
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, orderId.toString(), event)
                .orTimeout(KAFKA_SEND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .join();
    }

    private Order orderOf(UUID orderId) {
        return orderRepository.findByOrderId(orderId).orElseThrow();
    }

    private Order orderWithItemsOf(UUID orderId) {
        return orderRepository.findByOrderIdWithItems(orderId).orElseThrow();
    }
}
