package com.bezrukov.orderservice.integrations;

import com.bezrukov.orderservice.dto.OrderItemRequest;
import com.bezrukov.orderservice.dto.OrderRequest;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.OutboxMessage;
import com.bezrukov.orderservice.entity.Status;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import com.bezrukov.orderservice.reposiroty.OutboxRepository;
import com.bezrukov.orderservice.reposiroty.UserRepository;
import com.bezrukov.orderservice.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    private static final String EVENT_TYPE_RESERVE_STOCK = "ORDER_RESERVE_COMMAND";
    private static final long DEFAULT_PRODUCT_ID = 1L;
    private static final long DEFAULT_QUANTITY = 2L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser-" + UUID.randomUUID())
                .password("encoded-password")
                .email("test-" + UUID.randomUUID() + "@example.com")
                .build();
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("При создании заказа он сохраняется в БД и в Outbox")
    void shouldCreateOrderAndSaveToOutbox() {
        String idempotencyKey = randomIdempotencyKey();
        OrderRequest request = orderRequest(idempotencyKey);

        Order order = orderService.createOrder(request, testUser.getId());

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(Status.PENDING);
        assertThat(order.getUser().getId()).isEqualTo(testUser.getId());

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(Status.PENDING);
        assertThat(savedOrder.getIdempotencyKey()).isEqualTo(idempotencyKey);

        List<OutboxMessage> outboxMessages = outboxRepository.findAll();
        assertThat(outboxMessages).hasSize(1);

        OutboxMessage outboxMessage = outboxMessages.getFirst();
        assertThat(outboxMessage.getEventType()).isEqualTo(EVENT_TYPE_RESERVE_STOCK);
        assertThat(outboxMessage.getAggregateId()).isEqualTo(order.getId());
        assertThat(outboxMessage.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(outboxMessage.isProcessed()).isFalse();
    }

    @Test
    @DisplayName("При повторном запросе возвращается существующий заказ")
    void shouldReturnExistingOrderForIdempotentRequest() {
        OrderRequest request = orderRequest(randomIdempotencyKey());

        Order firstOrder = orderService.createOrder(request, testUser.getId());
        Order secondOrder = orderService.createOrder(request, testUser.getId());

        assertThat(secondOrder.getId()).isEqualTo(firstOrder.getId());

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);
    }


    @Test
    @DisplayName("При несуществующем пользователе выбрасывается исключение")
    void shouldThrowExceptionWhenUserNotFound() {
        UUID nonExistentUserId = UUID.randomUUID();
        OrderRequest request = orderRequest(randomIdempotencyKey());

        assertThatThrownBy(() -> orderService.createOrder(request, nonExistentUserId))
                .isInstanceOf(RuntimeException.class);

        assertThat(orderRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }

    private String randomIdempotencyKey() {
        return "test-key-" + UUID.randomUUID();
    }

    private OrderRequest orderRequest(String idempotencyKey) {
        return OrderRequest.builder()
                .idempotencyKey(idempotencyKey)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(DEFAULT_PRODUCT_ID)
                                .quantity(DEFAULT_QUANTITY)
                                .build()
                ))
                .build();
    }
}
