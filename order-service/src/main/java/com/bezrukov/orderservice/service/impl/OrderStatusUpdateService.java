package com.bezrukov.orderservice.service.impl;

import com.bezrukov.common.dto.ReservedItemDto;
import com.bezrukov.common.event.StockReservedEvent;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.OrderItem;
import com.bezrukov.orderservice.entity.Status;
import com.bezrukov.orderservice.kafka.OrderEventProducer;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderStatusUpdateService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    @WithSpan("reserved.event")
    public void handleStockReservedEvent(StockReservedEvent event) {
        UUID orderId = event.getOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (event.isSuccess()) {
            if (order.getStatus() == Status.CONFIRMED) {
                log.warn("Order already confirmed: {}", order.getId());
                return;
            }

            Double totalPrice = event.getItems().stream()
                    .mapToDouble(ReservedItemDto::getTotalPrice)
                    .sum();
            order.setTotalPrice(totalPrice);
            order.setStatus(Status.CONFIRMED);

            List<OrderItem> orderItems = event.getItems().stream().map(
                    item -> OrderItem.builder()
                            .order(order)
                            .productId(item.getId())
                            .price(item.getPrice())
                            .salePercent(item.getSalePercent())
                            .quantity(item.getQuantity())
                            .build()
            ).collect(Collectors.toCollection(ArrayList::new));
            order.getItems().clear();
            order.getItems().addAll(orderItems);

            log.info("Order confirmed: {}", orderId);
            orderEventProducer.sendOrderConfirmedEvent(order);
        } else {
            order.setStatus(Status.REJECTED);
            log.warn("Order rejected: {}, reason: {}", orderId, event.getMessage());
        }

        orderRepository.save(order);
    }
}
