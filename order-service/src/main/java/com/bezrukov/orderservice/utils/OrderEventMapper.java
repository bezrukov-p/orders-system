package com.bezrukov.orderservice.utils;

import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.OrderItem;
import event.OrderCreatedEvent;
import event.OrderItemEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventMapper {

    public OrderCreatedEvent toOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .createdAt(order.getCreatedAt())
                .build();

        List<OrderItemEvent> items = order.getItems().stream()
                .map(this::toOrderItemEvent)
                .toList();

        event.setItems(items);
        return event;
    }

    public OrderItemEvent toOrderItemEvent(OrderItem orderItem) {
        return OrderItemEvent.builder()
                .productId(orderItem.getProductId())
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .salePercent(orderItem.getSalePercent())
                .build();
    }
}
