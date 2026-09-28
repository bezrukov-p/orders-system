package com.bezrukov.notificationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с информацией о позиции заказа")
public record OrderItemResponse(

        @Schema(description = "Локальный ID позиции", example = "1")
        Long id,

        @Schema(description = "ID товара", example = "1")
        Long productId,

        @Schema(description = "Наименование товара", example = "MacBook")
        String name,

        @Schema(description = "Количество товара", example = "2")
        Long quantity,

        @Schema(description = "Цена товара на момент заказа", example = "999.99")
        Double price,

        @Schema(description = "Процент скидки", example = "10")
        Integer salePercent
) {
}
