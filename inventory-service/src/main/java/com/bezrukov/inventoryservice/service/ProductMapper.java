package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
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
                .price(product.getPrice().doubleValue())
                .quantity(product.getQuantity())
                .salePercent(product.getSalePercent())
                .build();
    }

    public Product toEntity(ProductCreateRequest request) {
        if (request == null) {
            return null;
        }

        return Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .salePercent(request.getSalePercent())
                .build();
    }
}
