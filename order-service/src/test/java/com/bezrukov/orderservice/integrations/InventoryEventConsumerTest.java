package com.bezrukov.orderservice.integrations;

import com.bezrukov.common.dto.ReservedItemDto;
import com.bezrukov.common.event.StockReservedEvent;
import com.bezrukov.orderservice.entity.*;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import com.bezrukov.orderservice.reposiroty.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class InventoryEventConsumerTest {

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

    @Test
    @DisplayName("При success=true заказ должен стать CONFIRMED")
    void shouldUpdateOrderStatusToConfirmed() {
        Order order = createOrder(Status.PENDING);
        UUID orderId = order.getId();

        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(order.getId())
                .success(true)
                .idempotencyKey("test-key")
                .items(List.of(
                        ReservedItemDto.builder()
                                .id(1L)
                                .quantity(2L)
                                .price(999.99)
                                .totalPrice(1999.98)
                                .build()
                ))
                .build();
        kafkaTemplate.send("inventory-events", order.getId().toString(), event);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order updated = orderRepository.findById(order.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(Status.CONFIRMED);
                    assertThat(updated.getTotalPrice()).isEqualTo(1999.98);
                });
    }

    @Test
    @DisplayName("При success=false заказ должен стать REJECTED")
    void shouldUpdateOrderStatusToRejected() {
        Order order = createOrder(Status.PENDING);
        UUID orderId = order.getId();

        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(orderId)
                .success(false)
                .idempotencyKey(order.getIdempotencyKey())
                .message("Insufficient stock for products: [InsufficientStockItem(productId=1, requestedQuantity=50, availableQuantity=45)]")
                .build();
        kafkaTemplate.send("inventory-events", orderId.toString(), event);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order updated = orderRepository.findById(orderId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(Status.REJECTED);
                    assertThat(updated.getTotalPrice()).isEqualTo(0.0);
                });
    }

    @Test
    @DisplayName("Повторное событие не должно менять статус (идемпотентность)")
    void shouldIgnoreDuplicateEvent() {
        Order order = createOrder(Status.CONFIRMED);
        UUID orderId = order.getId();

        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(orderId)
                .success(true)
                .idempotencyKey(order.getIdempotencyKey())
                .items(List.of(
                        ReservedItemDto.builder()
                                .id(1L)
                                .quantity(2L)
                                .price(999.99)
                                .totalPrice(1999.98)
                                .build()
                ))
                .build();
        kafkaTemplate.send("inventory-events", orderId.toString(), event);

        await().during(2, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order updated = orderRepository.findById(orderId).orElseThrow();
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
                .items(List.of(
                        ReservedItemDto.builder()
                                .id(1L)
                                .quantity(2L)
                                .price(999.99)
                                .totalPrice(1999.98)
                                .build()
                ))
                .build();
        kafkaTemplate.send("inventory-events", nonExistentOrderId.toString(), event);

        await().during(2, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(orderRepository.findById(nonExistentOrderId)).isEmpty();
                });
    }

    @Test
    @DisplayName("При CONFIRMED items заказа обновляются")
    void shouldUpdateOrderItemsOnConfirm() {
        Order order = createOrder(Status.PENDING);
        UUID orderId = order.getId();

        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(orderId)
                .success(true)
                .idempotencyKey(order.getIdempotencyKey())
                .items(List.of(
                        ReservedItemDto.builder()
                                .id(1L)
                                .quantity(2L)
                                .price(999.99)
                                .salePercent(0)
                                .totalPrice(1999.98)
                                .build(),
                        ReservedItemDto.builder()
                                .id(2L)
                                .quantity(1L)
                                .price(49.99)
                                .salePercent(10)
                                .totalPrice(44.99)
                                .build()
                ))
                .build();
        kafkaTemplate.send("inventory-events", orderId.toString(), event);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Order updated = orderRepository.findByIdWithItems(orderId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(Status.CONFIRMED);
                    assertThat(updated.getItems()).hasSize(2);
                    assertThat(updated.getItems())
                            .extracting(OrderItem::getProductId)
                            .containsExactlyInAnyOrder(1L, 2L);
                    assertThat(updated.getTotalPrice()).isEqualTo(2044.97);
                });
    }

    private Order createOrder(Status status) {
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(OrderItem.builder()
                .productId(1L)
                .quantity(1L)
                .price(1.0)
                .salePercent(0)
                .build());

        Order order = Order.builder()
                .user(testUser)
                .items(orderItems)
                .status(status)
                .totalPrice(0.0)
                .idempotencyKey("test-key-" + UUID.randomUUID())
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        return orderRepository.save(order);
    }
}
