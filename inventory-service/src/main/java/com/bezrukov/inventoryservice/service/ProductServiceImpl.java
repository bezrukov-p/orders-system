package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
import com.bezrukov.inventoryservice.entity.Product;
import com.bezrukov.inventoryservice.exception.ProductNotFoundException;
import com.bezrukov.inventoryservice.repository.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> findAll() {
        log.debug("find all products");
        return productRepository.findAll();
    }

    @Override
    public Product findById(Long id) {
        log.debug("find product by id: {}", id);
        return productRepository.findById(id).orElseThrow(
                () -> new ProductNotFoundException("Product not found"));
    }

    @Override
    public Product create(ProductCreateRequest productCreateRequest) {
        log.debug("create product: {}", productCreateRequest.getName());
        Product product = productRepository.save(Product.builder()
                .name(productCreateRequest.getName())
                .price(productCreateRequest.getPrice())
                .quantity(productCreateRequest.getQuantity())
                .salePercent(productCreateRequest.getSalePercent())
                .build());
        log.info("product created: {}", product);
        return product;
    }

    @Override
    public void deleteById(Long id) {
        log.debug("delete product by id: {}", id);
        productRepository.deleteById(id);
        log.info("product deleted: {}", id);
    }
}
