package com.bezrukov.notificationservice.service;


import com.bezrukov.common.event.OrderConfirmedEvent;
import com.bezrukov.common.event.OrderItemEvent;
import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.entity.OrderItem;
import com.bezrukov.notificationservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderProcessingService: Сохранение подтвержденных заказов в Notification DB")
class OrderProcessingServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderProcessingServiceImpl orderProcessingService;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "test@example.com";
    private static final Double TOTAL_PRICE = 230.0;
    private static final LocalDateTime CREATED_AT = LocalDateTime.now();

    private OrderConfirmedEvent event;
    private List<OrderItemEvent> itemEvents;

    @BeforeEach
    void setUp() {
        itemEvents = List.of(
                OrderItemEvent.builder()
                        .productId(1L)
                        .quantity(2L)
                        .price(100.0)
                        .salePercent(10)
                        .build(),
                OrderItemEvent.builder()
                        .productId(2L)
                        .quantity(1L)
                        .price(50.0)
                        .salePercent(0)
                        .build()
        );

        event = OrderConfirmedEvent.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .userEmail(USER_EMAIL)
                .totalPrice(TOTAL_PRICE)
                .createdAt(CREATED_AT)
                .items(itemEvents)
                .build();
    }

    @Nested
    @DisplayName("УСПЕШНЫЕ СЦЕНАРИИ")
    class SuccessScenarios {

        @Test
        @DisplayName("При сохранении нового заказа должны создаваться Order и OrderItem")
        void shouldSaveNewOrderWithItems() {
            when(orderRepository.existsByOrderId(ORDER_ID))
                    .thenReturn(false);

            orderProcessingService.saveConfirmedOrder(event);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(savedOrder.getUserId()).isEqualTo(USER_ID);
            assertThat(savedOrder.getUserEmail()).isEqualTo(USER_EMAIL);
            assertThat(savedOrder.getTotalPrice()).isEqualTo(TOTAL_PRICE);
            assertThat(savedOrder.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(savedOrder.getStatus()).isEqualTo("CONFIRMED");

            assertThat(savedOrder.getItems()).hasSize(2);

            OrderItem firstItem = savedOrder.getItems().getFirst();
            assertThat(firstItem.getProductId()).isEqualTo(1L);
            assertThat(firstItem.getQuantity()).isEqualTo(2L);
            assertThat(firstItem.getPrice()).isEqualTo(100.0);
            assertThat(firstItem.getSalePercent()).isEqualTo(10);

            OrderItem secondItem = savedOrder.getItems().get(1);
            assertThat(secondItem.getProductId()).isEqualTo(2L);
            assertThat(secondItem.getQuantity()).isEqualTo(1L);
            assertThat(secondItem.getPrice()).isEqualTo(50.0);
            assertThat(secondItem.getSalePercent()).isEqualTo(0);

            assertThat(firstItem.getOrder()).isEqualTo(savedOrder);
            assertThat(secondItem.getOrder()).isEqualTo(savedOrder);
        }

        @Test
        @DisplayName("Order и OrderItem должны быть правильно связаны между собой")
        void shouldSetCorrectRelationshipBetweenOrderAndItems() {
            when(orderRepository.existsByOrderId(ORDER_ID))
                    .thenReturn(false);

            orderProcessingService.saveConfirmedOrder(event);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getItems())
                    .allMatch(item -> item.getOrder() == savedOrder);
        }
    }

    @Nested
    @DisplayName("ИДЕМПОТЕНТНОСТЬ")
    class IdempotencyScenarios {

        @Test
        @DisplayName("Если заказ уже существует, он не должен сохраняться повторно")
        void shouldNotSaveOrderWhenAlreadyExists() {
            when(orderRepository.existsByOrderId(ORDER_ID))
                    .thenReturn(true);

            orderProcessingService.saveConfirmedOrder(event);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("При повторном сохранении заказа не должно создаваться новых позиций")
        void shouldNotCreateNewItemsForDuplicateOrder() {
            when(orderRepository.existsByOrderId(ORDER_ID))
                    .thenReturn(true);

            orderProcessingService.saveConfirmedOrder(event);

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("ПОГРАНИЧНЫЕ СЦЕНАРИИ")
    class EdgeCases {

        @Test
        @DisplayName("Заказ без позиций должен сохраняться с пустым списком items")
        void shouldSaveOrderWithEmptyItems() {
            OrderConfirmedEvent emptyEvent = OrderConfirmedEvent.builder()
                    .orderId(ORDER_ID)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .totalPrice(TOTAL_PRICE)
                    .createdAt(CREATED_AT)
                    .items(List.of())
                    .build();

            when(orderRepository.existsByOrderId(ORDER_ID))
                    .thenReturn(false);

            orderProcessingService.saveConfirmedOrder(emptyEvent);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Order с одним товаром должен сохраняться корректно")
        void shouldSaveOrderWithSingleItem() {
            List<OrderItemEvent> singleItem = List.of(
                    OrderItemEvent.builder()
                            .productId(1L)
                            .quantity(5L)
                            .price(99.99)
                            .salePercent(0)
                            .build()
            );

            OrderConfirmedEvent singleItemEvent = OrderConfirmedEvent.builder()
                    .orderId(ORDER_ID)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .totalPrice(499.95)
                    .createdAt(CREATED_AT)
                    .items(singleItem)
                    .build();

            when(orderRepository.existsByOrderId(ORDER_ID))
                    .thenReturn(false);

            orderProcessingService.saveConfirmedOrder(singleItemEvent);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getItems()).hasSize(1);

            OrderItem item = savedOrder.getItems().getFirst();
            assertThat(item.getProductId()).isEqualTo(1L);
            assertThat(item.getQuantity()).isEqualTo(5L);
            assertThat(item.getPrice()).isEqualTo(99.99);
        }

        @Test
        @DisplayName("Поля totalPrice и createdAt должны корректно сохраняться")
        void shouldSaveTotalPriceAndCreatedAtCorrectly() {
            Double expectedTotal = 999.99;
            LocalDateTime expectedCreatedAt = LocalDateTime.of(2026, 8, 3, 12, 0, 0);

            OrderConfirmedEvent eventWithData = OrderConfirmedEvent.builder()
                    .orderId(ORDER_ID)
                    .userId(USER_ID)
                    .userEmail(USER_EMAIL)
                    .totalPrice(expectedTotal)
                    .createdAt(expectedCreatedAt)
                    .items(itemEvents)
                    .build();

            when(orderRepository.existsByOrderId(ORDER_ID))
                    .thenReturn(false);

            orderProcessingService.saveConfirmedOrder(eventWithData);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTotalPrice()).isEqualTo(expectedTotal);
            assertThat(savedOrder.getCreatedAt()).isEqualTo(expectedCreatedAt);
        }
    }
}