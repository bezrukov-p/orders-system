package com.bezrukov.notificationservice.listener;

import com.bezrukov.notificationservice.service.OrderProcessingService;
import event.OrderCreatedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class OrderEventListener {

    private final OrderProcessingService orderProcessingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "orders",
            groupId = "notification-group"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event);

        try {
            orderProcessingService.processOrder(event);
        } catch (Exception e) {
            log.error("Failed to process order: orderId={}, error={}",
                    event.getOrderId(),
                    e.getMessage(),
                    e
            );

            kafkaTemplate.send("orders.dlt", event);
            throw e;
        }
    }
}
