package com.bezrukov.notificationservice.utils;

import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.entity.OrderItem;
import event.OrderCreatedEvent;
import event.OrderItemEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderEventMapper {

    public Order toOrderEntity(OrderCreatedEvent event) {
        Order order = Order.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .userEmail(event.getUserEmail())
                .description(event.getDescription())
                .status(event.getStatus())
                .totalPrice(event.getTotalPrice())
                .createdAt(event.getCreatedAt())
                .receivedAt(LocalDateTime.now())
                .build();

        List<OrderItem> items = event.getItems().stream()
                .map(item -> toOrderItemEntity(item, order))
                .toList();

        order.setItems(items);
        return order;
    }

    public OrderItem toOrderItemEntity(OrderItemEvent itemEvent, Order order) {
        return OrderItem.builder()
                .order(order)
                .productId(itemEvent.getProductId())
                .quantity(itemEvent.getQuantity())
                .price(itemEvent.getPrice())
                .salePercent(itemEvent.getSalePercent())
                .build();
    }
}
