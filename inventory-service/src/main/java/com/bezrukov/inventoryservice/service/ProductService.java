package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
import com.bezrukov.inventoryservice.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> findAll();
    Product findById(Long id);
    Product create(ProductCreateRequest productCreateRequest);
    void deleteById(Long id);
}
