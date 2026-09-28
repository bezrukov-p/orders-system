package com.bezrukov.notificationservice.kafka;

import com.bezrukov.common.event.OrderConfirmedEvent;
import com.bezrukov.notificationservice.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderProcessingService orderProcessingService;

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    @Transactional
    public void handleOrderConfirmedEvent(OrderConfirmedEvent event, Acknowledgment ack) {
        log.info("Received OrderConfirmedEvent: orderId={}, userId={}",
                event.getOrderId(), event.getUserId());
        orderProcessingService.saveConfirmedOrder(event);
        ack.acknowledge();
    }
}
