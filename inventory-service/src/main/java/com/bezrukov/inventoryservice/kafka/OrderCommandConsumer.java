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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandConsumer {

    private final StockReservationService stockReservationService;
    private final InventoryEventProducer eventProducer;

    @KafkaListener(topics = "order-commands", groupId = "inventory-service-group")
    public void handleReserveStockCommand(ReserveStockCommand command) {
        log.info("Received ReserveStockCommand: orderId={}", command.getOrderId());

        try{
            StockReservedEvent event = stockReservationService.reserveStock(command);
            eventProducer.sendStockReservedEvent(event);
        } catch (ProductNotFoundException e) {
            log.error(e.getMessage());
            sendFailureEvent(command, e.getMessage());
        } catch (InsufficientStockException e) {
            log.warn(e.getMessage());
            sendFailureEvent(command, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate idempotencyKey for orderId={}", command.getOrderId());
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