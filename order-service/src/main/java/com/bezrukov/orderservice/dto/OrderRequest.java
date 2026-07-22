package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Запрос на создание заказа")
public class OrderRequest {

    @NotEmpty(message = "Заказ должен содержать хотя бы один товар")
    @Valid
    @Schema(
            description = "Список товаров в заказе",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1
    )
    private List<OrderItemRequest> items;
}
