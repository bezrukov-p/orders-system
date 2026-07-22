package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с информацией о доступности товара")
public record ProductAvailabilityResponse(

        @Schema(description = "ID товара", example = "1")
        Long productId,

        @Schema(description = "Доступное количество на складе", example = "50")
        Long quantity
) {
}
