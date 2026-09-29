package com.bezrukov.inventoryservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с информацией о товаре")
public class ProductResponse {

    @Schema(description = "ID товара", example = "1")
    private Long id;

    @Schema(description = "Название товара", example = "iPhone 15 Pro")
    private String name;

    @Schema(description = "Количество товара на складе", example = "100")
    private Long quantity;

    @Schema(description = "Цена товара", example = "999.99")
    private Double price;

    @Schema(description = "Процент скидки на товар", example = "10")
    private Integer salePercent;
}
