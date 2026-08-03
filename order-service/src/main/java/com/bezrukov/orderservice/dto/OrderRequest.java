package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Запрос на создание заказа")
@Builder
public class OrderRequest {

    @NotBlank(message = "Ключ идемпотентности обязателен")
    private String idempotencyKey;

    @NotEmpty(message = "Заказ должен содержать хотя бы один товар")
    @Schema(
            description = "Список товаров в заказе",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1
    )
    private List<OrderItemRequest> items;
}
