package com.bezrukov.orderservice.kafka;

import com.bezrukov.common.event.OrderConfirmedEvent;
import com.bezrukov.common.event.OrderItemEvent;
import com.bezrukov.orderservice.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventProducer {
    private static final String TOPIC = "order-events";
    private final KafkaTemplate<String, OrderConfirmedEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderConfirmedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderConfirmedEvent(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUser().getId()) //TODO user? или userid передавать в параметре
                .userEmail(order.getUser().getEmail())
                .items(order.getItems().stream() //TODO маппер общий сделать
                        .map(item -> OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .name(item.getName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .salePercent(item.getSalePercent())
                                .build())
                        .toList())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .build();

        kafkaTemplate.send(TOPIC, order.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("OrderConfirmedEvent sent to Notification Service: orderId={}",
                                order.getId());
                    } else {
                        log.error("Failed to send OrderConfirmedEvent: orderId={}, error={}",
                                order.getId(), ex.getMessage(), ex);
                    }
                });
    }
}
