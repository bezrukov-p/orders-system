package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductResponse;
import com.bezrukov.inventoryservice.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .salePercent(product.getSalePercent())
                .build();
    }
}
