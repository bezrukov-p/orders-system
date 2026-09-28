package com.bezrukov.notificationservice.service;

import com.bezrukov.notificationservice.dto.OrderItemResponse;
import com.bezrukov.notificationservice.dto.OrderResponse;
import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderId(),
                order.getUserId(),
                order.getUserEmail(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getReceivedAt(),
                order.getItems() != null
                        ? order.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList())
                        : List.of()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getPrice(),
                item.getSalePercent()
        );
    }
}
