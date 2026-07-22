package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ответ с информацией о заказе")
public record OrderResponse(

        @Schema(description = "Уникальный идентификатор заказа", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "ID пользователя, создавшего заказ", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID userId,

        @Schema(
                description = "Статус заказа",
                example = "CREATED"
        )
        String status,

        @Schema(description = "Общая стоимость заказа", example = "1999.98")
        Double totalPrice,

        @Schema(description = "Дата и время создания заказа", example = "2026-07-22T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Список позиций в заказе")
        List<OrderItemResponse> items
) {
}
