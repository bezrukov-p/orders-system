package com.bezrukov.notificationservice.service;

import com.bezrukov.notificationservice.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<OrderResponse> getAllOrders();
    OrderResponse getOrderByOrderId(UUID orderId);
    List<OrderResponse> getOrdersByUserId(UUID orderId);
}
