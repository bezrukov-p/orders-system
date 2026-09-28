package com.bezrukov.notificationservice.service;

import com.bezrukov.notificationservice.entity.Order;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<Order> getAllOrders();
    Order getOrderByOrderId(UUID orderId);
    List<Order> getOrdersByUserId(UUID orderId);
}
