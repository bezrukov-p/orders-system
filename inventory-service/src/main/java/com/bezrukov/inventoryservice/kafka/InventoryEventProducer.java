package com.bezrukov.inventoryservice.kafka;

import com.bezrukov.common.event.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private static final String TOPIC = "inventory-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendStockReservedEvent(StockReservedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event).get();
            log.info("StockReservedEvent sent: orderId={}, success={}",
                    event.getOrderId(), event.isSuccess());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted sending StockReservedEvent: orderId=" + event.getOrderId(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(
                    "Failed to send StockReservedEvent: orderId=" + event.getOrderId(), e.getCause());
        }
    }
}
