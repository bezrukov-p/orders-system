package com.bezrukov.orderservice.kafka;

import com.bezrukov.common.event.ReserveStockCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCommandProducer {

    private static final String TOPIC = "order-commands";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendReserveStockCommand(ReserveStockCommand command) {
        kafkaTemplate.send(TOPIC, command.getOrderId().toString(), command)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("ReserveStockCommand sent: orderId={}", command.getOrderId());
                    } else {
                        log.error("Failed to send ReserveStockCommand: orderId={}, error={}",
                                command.getOrderId(), ex.getMessage(), ex);
                    }
                });
    }
}
