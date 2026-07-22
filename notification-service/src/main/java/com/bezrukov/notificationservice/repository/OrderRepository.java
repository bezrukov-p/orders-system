package com.bezrukov.notificationservice.repository;

import com.bezrukov.notificationservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderId(UUID orderId);
}
