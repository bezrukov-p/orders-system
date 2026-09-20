package com.bezrukov.notificationservice.repository;

import com.bezrukov.notificationservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderId(UUID orderId);
    Optional<Order> findByOrderId(UUID orderId);
    long countByOrderId(UUID orderId);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    Optional<Order> findByOrderIdWithItems(@Param("orderId") UUID orderId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId")
    List<Order> findByUserIdWithItems(@Param("userId") UUID userId);
}
