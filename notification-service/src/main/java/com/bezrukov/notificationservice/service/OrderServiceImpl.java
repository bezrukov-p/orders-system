package com.bezrukov.notificationservice.service;

import com.bezrukov.notificationservice.dto.OrderItemResponse;
import com.bezrukov.notificationservice.dto.OrderResponse;
import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.entity.OrderItem;
import com.bezrukov.notificationservice.exception.OrderNotFoundException;
import com.bezrukov.notificationservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;

    public List<OrderResponse> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderByOrderId(UUID orderId) {
        log.debug("Fetching order by orderId: {}", orderId);
        Order order = orderRepository.findByOrderIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(UUID userId) {
        log.debug("Fetching orders by userId: {}", userId);
        return orderRepository.findByUserIdWithItems(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order) {
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
