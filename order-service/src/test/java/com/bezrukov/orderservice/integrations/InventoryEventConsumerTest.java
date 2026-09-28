package com.bezrukov.orderservice.integrations;

import com.bezrukov.common.dto.ReservedItemDto;
import com.bezrukov.common.event.StockReservedEvent;
import com.bezrukov.orderservice.entity.*;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import com.bezrukov.orderservice.reposiroty.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class InventoryEventConsumerTest {

    private static final String INVENTORY_EVENTS_TOPIC = "inventory-events";

    private static final Duration KAFKA_SEND_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration AWAIT_POLL = Duration.ofMillis(100);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .username("testuser-" + UUID.randomUUID())
                .password("encoded-password")
                .email("test-" + UUID.randomUUID() + "@example.com")
                .build());
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("При success=true заказ должен стать CONFIRMED")
    void shouldUpdateOrderStatusToConfirmed() {
        Order order = saveOrderWithStatus(Status.PENDING);
        double totalPrice = 1999.98;
        StockReservedEvent event = successEvent(order, totalPrice);

        sendInventoryEvent(order.getId(), event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .ignoreException(NoSuchElementException.class)
                .untilAsserted(() -> {
                    Order updated = orderOf(order.getId());
                    assertThat(updated.getStatus()).isEqualTo(Status.CONFIRMED);
                    assertThat(updated.getTotalPrice()).isEqualTo(totalPrice);
                });
    }

    @Test
    @DisplayName("При success=false заказ должен стать REJECTED")
    void shouldUpdateOrderStatusToRejected() {
        Order order = saveOrderWithStatus(Status.PENDING);
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(order.getId())
                .success(false)
                .idempotencyKey(order.getIdempotencyKey())
                .message("Insufficient stock for products: [...]")
                .build();

        sendInventoryEvent(order.getId(), event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .ignoreException(NoSuchElementException.class)
                .untilAsserted(() -> {
                    Order updated = orderOf(order.getId());
                    assertThat(updated.getStatus()).isEqualTo(Status.REJECTED);
                    assertThat(updated.getTotalPrice()).isEqualTo(0.0);
                });
    }

    @Test
    @DisplayName("Повторное событие не должно менять статус (идемпотентность)")
    void shouldIgnoreDuplicateEvent() {
        Order order = saveOrderWithStatus(Status.CONFIRMED);
        StockReservedEvent event = successEvent(order, 1999.98);

        sendInventoryEvent(order.getId(), event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .ignoreException(NoSuchElementException.class)
                .untilAsserted(() -> {
                    Order updated = orderOf(order.getId());
                    assertThat(updated.getStatus()).isEqualTo(Status.CONFIRMED);
                    assertThat(updated.getTotalPrice()).isEqualTo(0.0);
                });
    }

    @Test
    @DisplayName("Событие для несуществующего заказа игнорируется")
    void shouldIgnoreEventForNonExistentOrder() {
        UUID nonExistentOrderId = UUID.randomUUID();
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(nonExistentOrderId)
                .success(true)
                .idempotencyKey("non-existent-key")
                .items(List.of(reservedItem(1L, 2L, 999.99, 0, 1999.98)))
                .build();

        sendInventoryEvent(nonExistentOrderId, event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .untilAsserted(() ->
                        assertThat(orderRepository.findById(nonExistentOrderId)).isEmpty()
                );
    }

    @Test
    @DisplayName("При CONFIRMED items заказа обновляются")
    void shouldUpdateOrderItemsOnConfirm() {
        Order order = saveOrderWithStatus(Status.PENDING);
        double totalPrice = 2044.97;
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(order.getId())
                .success(true)
                .idempotencyKey(order.getIdempotencyKey())
                .items(List.of(
                        reservedItem(1L, 2L, 999.99, 0, 1999.98),
                        reservedItem(2L, 1L, 49.99, 10, 44.99)
                ))
                .build();

        sendInventoryEvent(order.getId(), event);

        await().atMost(AWAIT_TIMEOUT)
                .pollInterval(AWAIT_POLL)
                .ignoreException(NoSuchElementException.class)
                .untilAsserted(() -> {
                    Order updated = orderWithItemsOf(order.getId());
                    assertThat(updated.getStatus()).isEqualTo(Status.CONFIRMED);
                    assertThat(updated.getItems()).hasSize(2);
                    assertThat(updated.getItems())
                            .extracting(OrderItem::getProductId)
                            .containsExactlyInAnyOrder(1L, 2L);
                    assertThat(updated.getTotalPrice()).isEqualTo(totalPrice);
                });
    }

    private Order saveOrderWithStatus(Status status) {
        Order order = Order.builder()
                .user(testUser)
                .status(status)
                .totalPrice(0.0)
                .idempotencyKey("test-key-" + UUID.randomUUID())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(1L)
                .quantity(1L)
                .price(1.0)
                .salePercent(0)
                .build();

        order.getItems().add(item);

        return orderRepository.save(order);
    }

    private StockReservedEvent successEvent(Order order, double totalPrice) {
        return StockReservedEvent.builder()
                .orderId(order.getId())
                .success(true)
                .idempotencyKey(order.getIdempotencyKey())
                .items(List.of(reservedItem(1L, 2L, 999.99, 0, totalPrice)))
                .build();
    }

    private ReservedItemDto reservedItem(long id, long quantity, double price,
                                         int salePercent, double totalPrice) {
        return ReservedItemDto.builder()
                .id(id)
                .quantity(quantity)
                .price(price)
                .salePercent(salePercent)
                .totalPrice(totalPrice)
                .build();
    }

    private void sendInventoryEvent(UUID orderId, StockReservedEvent event) {
        kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, orderId.toString(), event)
                .orTimeout(KAFKA_SEND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .join();
    }

    private Order orderOf(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow();
    }

    private Order orderWithItemsOf(UUID orderId) {
        return orderRepository.findByIdWithItems(orderId).orElseThrow();
    }
}
