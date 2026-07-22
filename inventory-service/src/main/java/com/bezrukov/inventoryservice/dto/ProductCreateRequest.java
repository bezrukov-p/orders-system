package com.bezrukov.inventoryservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание товара")
public class ProductCreateRequest {

    @NotBlank(message = "Название товара обязательно")
    @Schema(
            description = "Название товара",
            example = "iPhone 15 Pro",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @NotNull(message = "Количество товара обязательно")
    @Min(value = 0, message = "Количество товара не может быть отрицательным")
    @Schema(
            description = "Количество товара на складе",
            example = "100",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long quantity;

    @NotNull(message = "Цена товара обязательна")
    @Positive(message = "Цена товара должна быть положительной")
    @Schema(
            description = "Цена товара",
            example = "999.99",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Double price;

    @Min(value = 0, message = "Скидка не может быть отрицательной")
    @Max(value = 100, message = "Скидка не может превышать 100%")
    @Schema(
            description = "Процент скидки на товар (0-100)",
            example = "10",
            defaultValue = "0"
    )
    private Integer salePercent;
}
