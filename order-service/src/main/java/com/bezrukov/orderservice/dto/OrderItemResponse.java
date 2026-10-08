package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ответ с информацией о позиции заказа")
public record OrderItemResponse(

        @Schema(description = "Уникальный идентификатор позиции заказа", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "ID товара", example = "1")
        Long productId,

        @Schema(description = "Название товара", example = "iPhone 15 Pro")
        String productName,

        @Schema(description = "Количество заказанного товара", example = "2")
        Long quantity,

        @Schema(description = "Цена товара на момент заказа", example = "999.99")
        Double price,

        @Schema(description = "Процент скидки на товар", example = "10")
        Integer salePercent
) {
}
