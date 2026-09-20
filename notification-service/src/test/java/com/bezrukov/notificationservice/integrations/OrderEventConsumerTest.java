package com.bezrukov.notificationservice.integrations;

import com.bezrukov.common.event.OrderConfirmedEvent;
import com.bezrukov.common.event.OrderItemEvent;
import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.repository.OrderRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderEventConsumerTest {

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
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .userEmail("test@example.com")
                .totalPrice(1999.98)
                .items(List.of(
                        OrderItemEvent.builder()
                                .productId(1L)
                                .quantity(2L)
                                .price(999.99)
                                .salePercent(0)
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send("order-events", orderId.toString(), event);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<Order> saved = orderRepository.findByOrderId(orderId);
                    assertThat(saved).isPresent();
                    assertThat(saved.get().getOrderId()).isEqualTo(orderId);
                    assertThat(saved.get().getTotalPrice()).isEqualTo(1999.98);
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

        kafkaTemplate.send("order-events", orderId.toString(), event);
        kafkaTemplate.send("order-events", orderId.toString(), event);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    long count = orderRepository.countByOrderId(orderId);
                    assertThat(count).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Заказ сохраняется с позициями")
    void shouldSaveOrderWithItems() {
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .totalPrice(2044.97)
                .items(List.of(
                        OrderItemEvent.builder()
                                .productId(1L)
                                .quantity(2L)
                                .price(999.99)
                                .salePercent(0)
                                .build(),
                        OrderItemEvent.builder()
                                .productId(2L)
                                .quantity(1L)
                                .price(49.99)
                                .salePercent(10)
                                .build()
                ))
                .build();

        kafkaTemplate.send("order-events", orderId.toString(), event);

        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<Order> saved = orderRepository.findByOrderIdWithItems(orderId);
                    assertThat(saved).isPresent();
                    assertThat(saved.get().getItems()).hasSize(2);
                    assertThat(saved.get().getTotalPrice()).isEqualTo(2044.97);
                });
    }
}
