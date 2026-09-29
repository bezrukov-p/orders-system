package com.bezrukov.orderservice.exceptions;

import java.util.UUID;

public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException(UUID orderId, UUID userId) {
        super("Нет доступа к заказу " + orderId + " для пользователя " + userId);
    }
}
