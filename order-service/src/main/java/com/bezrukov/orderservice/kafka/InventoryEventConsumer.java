package com.bezrukov.orderservice.kafka;

import com.bezrukov.common.event.StockReservedEvent;
import com.bezrukov.orderservice.service.impl.OrderStatusUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final OrderStatusUpdateService orderStatusUpdateService;

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    public void handleStockReservedEvent(StockReservedEvent event, Acknowledgment ack) {
        try {
            log.info("Received StockReservedEvent: orderId={}, success={}",
                    event.getOrderId(), event.isSuccess());
            orderStatusUpdateService.handleStockReservedEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to handle StockReservedEvent: orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}
