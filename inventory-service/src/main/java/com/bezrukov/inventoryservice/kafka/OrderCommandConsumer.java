package com.bezrukov.inventoryservice.kafka;

import com.bezrukov.common.event.ReserveStockCommand;
import com.bezrukov.common.event.StockReservedEvent;
import com.bezrukov.inventoryservice.exception.InsufficientStockException;
import com.bezrukov.inventoryservice.exception.ProductNotFoundException;
import com.bezrukov.inventoryservice.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandConsumer {

    private final StockReservationService stockReservationService;
    private final InventoryEventProducer eventProducer;

    @KafkaListener(topics = "order-commands", groupId = "inventory-service-group")
    public void handleReserveStockCommand(ReserveStockCommand command, Acknowledgment ack) {
        log.info("Received ReserveStockCommand: orderId={}", command.getOrderId());

        try{
            StockReservedEvent event = stockReservationService.reserveStock(command);
            eventProducer.sendStockReservedEvent(event);
            ack.acknowledge();
        } catch (ProductNotFoundException e) {
            log.error("Product not found for orderId={}: {}", command.getOrderId(), e.getMessage(), e);
            sendFailureEvent(command, e.getMessage());
            ack.acknowledge();
        } catch (InsufficientStockException e) {
            log.warn("Insufficient stock for orderId={}: {}", command.getOrderId(), e.getMessage());
            sendFailureEvent(command, e.getMessage());
            ack.acknowledge();
        } catch (DataIntegrityViolationException e) { // может быть другая ошибка
            log.warn("Duplicate idempotencyKey for orderId={}: {}", command.getOrderId(), e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Unexpected error for orderId={}, {}", command.getOrderId(), e.getMessage());
            throw e;
        }
    }

    private void sendFailureEvent(ReserveStockCommand command, String errorMessage) {
        eventProducer.sendStockReservedEvent(
                StockReservedEvent.builder()
                        .orderId(command.getOrderId())
                        .idempotencyKey(command.getIdempotencyKey())
                        .success(false)
                        .message(errorMessage)
                        .build()
        );
    }
}