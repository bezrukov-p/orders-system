package com.bezrukov.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {
    private Long id;
    private String name;
    private Long quantity;
    private BigDecimal price;
    private Integer salePercent;
}
