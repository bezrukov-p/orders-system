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
@Schema(description = "Информация о товаре, которого недостаточно на складе")
public class InsufficientStockItem {

    @Schema(description = "ID товара", example = "1")
    private Long productId;

    @Schema(description = "Запрошенное количество", example = "10")
    private Long requestedQuantity;

    @Schema(description = "Доступное количество на складе", example = "5")
    private Long availableQuantity;
}
