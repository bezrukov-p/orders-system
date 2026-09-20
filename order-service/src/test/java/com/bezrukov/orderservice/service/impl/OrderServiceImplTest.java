package com.bezrukov.orderservice.service.impl;

import com.bezrukov.common.dto.OrderItemDto;
import com.bezrukov.common.event.ReserveStockCommand;
import com.bezrukov.orderservice.dto.OrderItemRequest;
import com.bezrukov.orderservice.dto.OrderRequest;
import com.bezrukov.orderservice.dto.OrderResponse;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.Status;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.kafka.OrderCommandProducer;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import com.bezrukov.orderservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCommandProducer orderCommandProducer;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID userId;
    private User user;
    private OrderRequest orderRequest;
    private List<OrderItemRequest> itemsRequest;
    private final String idempotencyKey = "key";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("testUser")
                .build();

        itemsRequest = List.of(
                OrderItemRequest.builder()
                        .productId(1L)
                        .quantity(2L)
                        .build(),
                OrderItemRequest.builder()
                        .productId(2L)
                        .quantity(1L)
                        .build()
        );

        orderRequest = OrderRequest.builder()
                .idempotencyKey(idempotencyKey)
                .items(itemsRequest)
                .build();
    }

    @Nested
    @DisplayName("Успешные сценарии")
    class SuccessScenarios {

        @Test
        @DisplayName("При создании заказа он должен сохраниться в БД и событие должно быть сохранено в Outbox")
        void shouldCreateOrderAndSaveToOutbox() {
            Order savedOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .user(user)
                    .status(Status.PENDING)
                    .totalPrice(0.0)
                    .build();

            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());
            when(userService.getReferenceById(userId))
                    .thenReturn(user);
            when(orderRepository.save(any(Order.class)))
                    .thenReturn(savedOrder);

            Order response = orderService.createOrder(orderRequest, userId);

            assertThat(response.getId()).isEqualTo(savedOrder.getId());
            assertThat(response.getStatus()).isEqualTo(Status.PENDING);
            assertThat(response.getUser().getId()).isEqualTo(userId);
            assertThat(response.getTotalPrice()).isEqualTo(0.0);

            verify(orderRepository, times(1)).save(any(Order.class));
            verify(userService, times(1)).getReferenceById(userId);

            // ✅ Проверяем, что событие сохранено в Outbox, а НЕ отправлено напрямую в Kafka
            verify(outboxService, times(1)).saveEvent(
                    eq(savedOrder.getId()),
                    eq("ORDER_RESERVE_COMMAND"),
                    any(ReserveStockCommand.class),
                    eq(idempotencyKey)
            );

            verify(orderCommandProducer, never())
                    .sendReserveStockCommand(any(ReserveStockCommand.class));
        }

        @Test
        @DisplayName("Событие в Outbox должно содержать правильные данные")
        void shouldSaveCorrectOutboxEvent() {
            Order savedOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .user(user)
                    .status(Status.PENDING)
                    .totalPrice(0.0)
                    .build();

            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());
            when(userService.getReferenceById(userId))
                    .thenReturn(user);
            when(orderRepository.save(any(Order.class)))
                    .thenReturn(savedOrder);

            orderService.createOrder(orderRequest, userId);

            ArgumentCaptor<ReserveStockCommand> commandCaptor =
                    ArgumentCaptor.forClass(ReserveStockCommand.class);

            verify(outboxService, times(1)).saveEvent(
                    eq(savedOrder.getId()),
                    eq("ORDER_RESERVE_COMMAND"),
                    commandCaptor.capture(),
                    eq(idempotencyKey)
            );

            ReserveStockCommand capturedCommand = commandCaptor.getValue();

            assertThat(capturedCommand.getOrderId()).isEqualTo(savedOrder.getId());
            assertThat(capturedCommand.getIdempotencyKey()).isEqualTo(idempotencyKey);
            assertThat(capturedCommand.getItems()).hasSize(2);

            OrderItemDto firstItem = capturedCommand.getItems().getFirst();
            assertThat(firstItem.getProductId()).isEqualTo(1L);
            assertThat(firstItem.getQuantity()).isEqualTo(2L);

            OrderItemDto secondItem = capturedCommand.getItems().get(1);
            assertThat(secondItem.getProductId()).isEqualTo(2L);
            assertThat(secondItem.getQuantity()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Сохранение в Outbox должно происходить в той же транзакции, что и сохранение заказа")
        void shouldSaveOutboxInSameTransaction() {
            Order savedOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .user(user)
                    .status(Status.PENDING)
                    .totalPrice(0.0)
                    .build();

            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());
            when(userService.getReferenceById(userId))
                    .thenReturn(user);
            when(orderRepository.save(any(Order.class)))
                    .thenReturn(savedOrder);

            orderService.createOrder(orderRequest, userId);

            InOrder inOrder = inOrder(orderRepository, outboxService);

            inOrder.verify(orderRepository).save(any(Order.class));
            inOrder.verify(outboxService).saveEvent(
                    eq(savedOrder.getId()),
                    eq("ORDER_RESERVE_COMMAND"),
                    any(ReserveStockCommand.class),
                    eq(idempotencyKey)
            );
        }
    }

    @Nested
    @DisplayName("Сценарии идемпотентности")
    class IdempotencyScenarios {

        @Test
        @DisplayName("При повторном запросе с тем же idempotencyKey должен вернуться существующий заказ")
        void shouldReturnExistingOrderForIdempotentRequest() {
            UUID existingOrderId = UUID.randomUUID();

            Order existingOrder = Order.builder()
                    .id(existingOrderId)
                    .idempotencyKey(idempotencyKey)
                    .user(user)
                    .status(Status.PENDING)
                    .totalPrice(0.0)
                    .build();

            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingOrder));

            Order response = orderService.createOrder(orderRequest, userId);

            assertThat(response.getId()).isEqualTo(existingOrderId);
            assertThat(response.getStatus()).isEqualTo(Status.PENDING);

            verify(orderRepository, never()).save(any(Order.class));
            verify(userService, never()).getReferenceById(any());

            verify(outboxService, never()).saveEvent(any(), any(), any(), anyString());
            verify(orderCommandProducer, never())
                    .sendReserveStockCommand(any(ReserveStockCommand.class));
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    class ErrorScenarios {

        @Test
        @DisplayName("Если пользователь не найден, должно выбрасываться исключение")
        void shouldThrowExceptionWhenUserNotFound() {
            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());

            when(userService.getReferenceById(userId))
                    .thenThrow(new RuntimeException("User not found"));

            assertThatThrownBy(() -> orderService.createOrder(orderRequest, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(orderRepository, never()).save(any(Order.class));
            verify(outboxService, never()).saveEvent(any(), any(), any(), anyString());
            verify(orderCommandProducer, never())
                    .sendReserveStockCommand(any(ReserveStockCommand.class));
        }

        @Test
        @DisplayName("Если orderRepository.save() выбрасывает исключение, событие не должно сохраняться в Outbox")
        void shouldNotSaveOutboxWhenOrderSaveFails() {
            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());
            when(userService.getReferenceById(userId))
                    .thenReturn(user);
            when(orderRepository.save(any(Order.class)))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> orderService.createOrder(orderRequest, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");

            verify(outboxService, never()).saveEvent(any(), any(), any(), anyString());
            verify(orderCommandProducer, never())
                    .sendReserveStockCommand(any(ReserveStockCommand.class));
        }

        @Test
        @DisplayName("Если outboxService.saveEvent() выбрасывает исключение, транзакция должна откатиться")
        void shouldRollbackTransactionWhenOutboxSaveFails() {
            Order savedOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .user(user)
                    .status(Status.PENDING)
                    .totalPrice(0.0)
                    .build();

            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());
            when(userService.getReferenceById(userId))
                    .thenReturn(user);
            when(orderRepository.save(any(Order.class)))
                    .thenReturn(savedOrder);
            doThrow(new RuntimeException("Outbox save failed"))
                    .when(outboxService).saveEvent(any(), any(), any(), anyString());

            assertThatThrownBy(() -> orderService.createOrder(orderRequest, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Outbox save failed");

            verify(orderRepository, times(1)).save(any(Order.class));

            verify(outboxService, times(1)).saveEvent(
                    eq(savedOrder.getId()),
                    eq("ORDER_RESERVE_COMMAND"),
                    any(ReserveStockCommand.class),
                    eq(idempotencyKey)
            );

            verify(orderCommandProducer, never())
                    .sendReserveStockCommand(any(ReserveStockCommand.class));
        }
    }
}