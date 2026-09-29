package com.bezrukov.orderservice.service.impl;

import com.bezrukov.common.dto.ReservedItemDto;
import com.bezrukov.common.event.StockReservedEvent;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.Status;
import com.bezrukov.orderservice.kafka.OrderEventProducer;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusUpdateServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderStatusUpdateService service;

    private UUID orderId;
    private Order order;
    private StockReservedEvent successEvent;
    private StockReservedEvent failedEvent;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        order = Order.builder()
                .id(orderId)
                .status(Status.PENDING)
                .totalPrice(0.0)
                .items(new ArrayList<>())
                .build();

        successEvent = StockReservedEvent.builder()
                .orderId(orderId)
                .success(true)
                .items(List.of(
                        ReservedItemDto.builder()
                                .id(1L)
                                .quantity(2L)
                                .price(100.0)
                                .salePercent(10)
                                .totalPrice(180.0)
                                .build(),
                        ReservedItemDto.builder()
                                .id(2L)
                                .quantity(1L)
                                .price(50.0)
                                .salePercent(0)
                                .totalPrice(50.0)
                                .build()
                ))
                .build();

        failedEvent = StockReservedEvent.builder()
                .orderId(orderId)
                .success(false)
                .message("Insufficient stock")
                .build();
    }

    @AfterEach
    void tearDown() {
    }

    @Nested
    @DisplayName("УСПЕШНЫЕ СЦЕНАРИИ")
    class SuccessScenarios {

        @Test
        @DisplayName("При успешном резервировании заказ должен перейти в статус CONFIRMED, обновить totalPrice и items")
        void shouldConfirmOrderWhenStockAvailable() {
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            service.handleStockReservedEvent(successEvent);

            assertThat(order.getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(order.getTotalPrice()).isEqualTo(230.0);
            assertThat(order.getItems()).hasSize(2);

            verify(orderRepository, times(1)).save(order);
            verify(orderEventProducer, times(1))
                    .sendOrderConfirmedEvent(order);
        }

        @Test
        @DisplayName("OrderEventProducer должен быть вызван с правильным Order (содержащим все обновленные поля)")
        void shouldSendOrderConfirmedEventWithCorrectOrder() {
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            service.handleStockReservedEvent(successEvent);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderEventProducer, times(1))
                    .sendOrderConfirmedEvent(orderCaptor.capture());

            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getId()).isEqualTo(orderId);
            assertThat(capturedOrder.getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(capturedOrder.getTotalPrice()).isEqualTo(230.0);
            assertThat(capturedOrder.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("OrderRepository должен сохранить заказ с обновленными полями")
        void shouldSaveOrderWithUpdatedFields() {
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            service.handleStockReservedEvent(successEvent);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(savedOrder.getTotalPrice()).isEqualTo(230.0);
            assertThat(savedOrder.getItems()).hasSize(2);
        }
    }


    @Nested
    @DisplayName("СЦЕНАРИИ С ОШИБКАМИ")
    class ErrorScenarios {

        @Test
        @DisplayName("При failure = false заказ должен перейти в статус REJECTED, без отправки событий")
        void shouldRejectOrderWhenStockNotAvailable() {
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            service.handleStockReservedEvent(failedEvent);

            assertThat(order.getStatus()).isEqualTo(Status.REJECTED);
            assertThat(order.getItems()).isEmpty();

            verify(orderRepository, times(1)).save(order);
            verify(orderEventProducer, never()).sendOrderConfirmedEvent(order);
        }

        @Test
        @DisplayName("Если заказ не найден в БД, должно выбрасываться исключение")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.handleStockReservedEvent(successEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Order not found");

            verify(orderRepository, never()).save(any());
            verify(orderEventProducer, never()).sendOrderConfirmedEvent(any());
        }
    }


    @Nested
    @DisplayName("ПОГРАНИЧНЫЕ СЦЕНАРИИ")
    class EdgeCases {

        @Test
        @DisplayName("Если заказ уже в статусе CONFIRMED, повторная обработка должна игнорироваться")
        void shouldIgnoreWhenOrderAlreadyConfirmed() {
            order.setStatus(Status.CONFIRMED);
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            service.handleStockReservedEvent(successEvent);

            assertThat(order.getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(order.getTotalPrice()).isEqualTo(0.0);
            assertThat(order.getItems()).isEmpty();

            verify(orderEventProducer, never()).sendOrderConfirmedEvent(order);
            verify(orderRepository, never()).save(order);
        }

        @Test
        @DisplayName("Расчет totalPrice должен корректно суммировать все позиции заказа")
        void shouldCalculateTotalPriceCorrectly() {
            when(orderRepository.findById(orderId))
                    .thenReturn(Optional.of(order));

            service.handleStockReservedEvent(successEvent);

            double expectedTotal = successEvent.getItems().stream()
                    .mapToDouble(ReservedItemDto::getTotalPrice)
                    .sum();

            assertThat(order.getTotalPrice()).isEqualTo(expectedTotal);
            assertThat(order.getTotalPrice()).isEqualTo(230.0);
        }
    }
}