package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
import com.bezrukov.inventoryservice.dto.ProductResponse;
import com.bezrukov.inventoryservice.entity.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

public interface ProductService {

    @Operation(summary = "Получить все товары")
    List<ProductResponse> findAll();

    @Operation(summary = "Получить товар по ID")
    ProductResponse findById(
            @Parameter(description = "ID товара", required = true) Long id
    );

    @Operation(summary = "Создать новый товар")
    ProductResponse create(
            @Parameter(description = "Данные для создания товара", required = true)
            ProductCreateRequest productCreateRequest
    );

    @Operation(summary = "Удалить товар по ID")
    void deleteById(
            @Parameter(description = "ID товара", required = true) Long id
    );

    @Operation(summary = "Получить товар с проверкой наличия (для gRPC)")
    Product getProductWithAvailability(
            @Parameter(description = "ID товара", required = true) Long productId,
            @Parameter(description = "Запрошенное количество", required = true) Long quantity
    );
}
