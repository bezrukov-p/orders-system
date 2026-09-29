package com.bezrukov.notificationservice.service;

import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.exception.OrderNotFoundException;
import com.bezrukov.notificationservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;

    public List<Order> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll();
    }

    public Order getOrderByOrderId(UUID orderId) {
        log.debug("Fetching order by orderId: {}", orderId);
        return orderRepository.findByOrderIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    public List<Order> getOrdersByUserId(UUID userId) {
        log.debug("Fetching orders by userId: {}", userId);
        return orderRepository.findByUserIdWithItems(userId);
    }
}
