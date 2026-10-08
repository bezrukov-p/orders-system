package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.dto.OrderRequest;
import com.bezrukov.orderservice.entity.Order;

import java.util.UUID;

public interface OrderService {
    Order createOrder(OrderRequest orderRequest, UUID userId);

    Order getOrder(UUID orderId, UUID userId);
}
