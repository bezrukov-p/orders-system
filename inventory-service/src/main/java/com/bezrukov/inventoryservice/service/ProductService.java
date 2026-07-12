package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
import com.bezrukov.inventoryservice.dto.ProductResponse;
import com.bezrukov.inventoryservice.entity.Product;

import java.util.List;

public interface ProductService {

    List<ProductResponse> findAll();

    ProductResponse findById(Long id);

    ProductResponse create(ProductCreateRequest productCreateRequest);

    void deleteById(Long id);

    Product getProductWithAvailability(Long productId, Long quantity);
}
