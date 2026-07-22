package com.bezrukov.notificationservice.service;

import com.bezrukov.notificationservice.entity.Order;
import com.bezrukov.notificationservice.repository.OrderRepository;
import com.bezrukov.notificationservice.utils.OrderEventMapper;
import event.OrderCreatedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class OrderProcessingServiceImpl implements OrderProcessingService {

    private final OrderRepository orderRepository;
    private final OrderEventMapper orderEventMapper;

    @Override
    public void processOrder(OrderCreatedEvent event) {
        log.info("Processing Order Event: orderId={}", event.getOrderId());

        if (orderRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Order already processed: orderId={}", event.getOrderId());
            return;
        }

        Order order = orderEventMapper.toOrderEntity(event);
        orderRepository.save(order);

        log.info("Order saved successfully: orderId={}, items={}",
                event.getOrderId(),
                order.getItems().size()
        );
    }
}
