package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductRequest;
import com.bezrukov.inventoryservice.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    List<ProductResponse> findAll();

    ProductResponse findById(Long id);

    ProductResponse create(ProductRequest request);

    void deleteById(Long id);
}
