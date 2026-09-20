package com.bezrukov.notificationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ответ с информацией о заказе")
public record OrderResponse(

        @Schema(description = "Локальный ID заказа", example = "1")
        Long id,

        @Schema(description = "ID заказа из Order Service", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID orderId,

        @Schema(description = "ID пользователя", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID userId,

        @Schema(description = "Email пользователя", example = "user@example.com")
        String userEmail,

        @Schema(description = "Общая сумма заказа", example = "1999.98")
        Double totalPrice,

        @Schema(description = "Статус заказа", example = "CONFIRMED")
        String status,

        @Schema(description = "Дата создания заказа", example = "2026-09-16T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Дата получения заказа Notification Service", example = "2026-09-16T10:00:01")
        LocalDateTime receivedAt,

        @Schema(description = "Список позиций заказа")
        List<OrderItemResponse> items
) {
}
