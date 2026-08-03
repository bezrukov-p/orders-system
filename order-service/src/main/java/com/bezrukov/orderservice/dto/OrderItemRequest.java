package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание позиции заказа")
@Valid
@Builder
public class OrderItemRequest {

    @NotNull(message = "ID товара обязательно")
    @Schema(
            description = "ID товара",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long productId;

    @NotNull(message = "Количество товара обязательно")
    @Positive(message = "Количество товара должно быть положительным числом")
    @Schema(
            description = "Количество товара",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long quantity;
}
