package com.bezrukov.orderservice.kafka;

import com.bezrukov.common.event.ReserveStockCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCommandProducer {

    private static final String TOPIC = "order-commands";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendReserveStockCommand(ReserveStockCommand command) {
        try {
            kafkaTemplate.send(TOPIC, command.getOrderId().toString(), command)
                    .get();
            log.info("ReserveStockCommand sent: orderId={}", command.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while sending ReserveStockCommand: orderId=" + command.getOrderId(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(
                    "Failed to send ReserveStockCommand: orderId=" + command.getOrderId(),
                    e.getCause());
        }
    }
}
