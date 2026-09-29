package com.bezrukov.orderservice.exceptions;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super("Заказ не найден: " + orderId);
    }
}
