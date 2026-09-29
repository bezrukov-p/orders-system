package com.bezrukov.notificationservice.service;

import com.bezrukov.common.event.OrderConfirmedEvent;
import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.entity.OrderItem;
import com.bezrukov.notificationservice.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class OrderProcessingServiceImpl implements OrderProcessingService {

    private final OrderRepository orderRepository;

    @Override
    public void saveConfirmedOrder(OrderConfirmedEvent event) {
        if (orderRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Order already exists in Notification DB: orderId={}", event.getOrderId());
            return;
        }

        Order order = Order.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .userEmail(event.getUserEmail())
                .totalPrice(event.getTotalPrice())
                .createdAt(event.getCreatedAt())
                .status("CONFIRMED")
                .build();
        List<OrderItem> items = event.getItems().stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .productId(item.getProductId())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .salePercent(item.getSalePercent())
                        .build())
                .toList();
        order.setItems(items);
        orderRepository.save(order);

        log.info("Order saved to Notification DB: orderId={}", event.getOrderId());
    }
}
