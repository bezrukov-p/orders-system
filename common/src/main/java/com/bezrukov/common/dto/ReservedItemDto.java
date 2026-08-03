package com.bezrukov.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservedItemDto {
    private Long id;
    private String name;
    private Double price;
    private Integer salePercent;
    private Long quantity;
    private Double totalPrice;
}
