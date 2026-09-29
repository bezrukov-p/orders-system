package com.bezrukov.inventoryservice.kafka;

import com.bezrukov.common.event.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private static final String TOPIC = "inventory-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendStockReservedEvent(StockReservedEvent event) {
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("StockReservedEvent sent: orderId={}, success={}",
                                event.getOrderId(), event.isSuccess());
                    } else {
                        log.error("Failed to send StockReservedEvent: orderId={}, error={}",
                                event.getOrderId(), ex.getMessage(), ex);
                    }
                });
    }
}
