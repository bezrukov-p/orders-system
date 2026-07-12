package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
import com.bezrukov.inventoryservice.dto.ProductResponse;
import com.bezrukov.inventoryservice.entity.Product;
import com.bezrukov.inventoryservice.exception.ProductNotFoundException;
import com.bezrukov.inventoryservice.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(productMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ProductNotFoundException("Product not found"));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse create(ProductCreateRequest productCreateRequest) {
        Product product = productRepository.save(Product.builder()
                .name(productCreateRequest.getName())
                .price(productCreateRequest.getPrice())
                .quantity(productCreateRequest.getQuantity())
                .salePercent(productCreateRequest.getSalePercent())
                .build());
        return productMapper.toResponse(product);
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public Product getProductWithAvailability(Long productId, Long quantity) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found with id " + productId)
        );
        if (product.getQuantity() < quantity) {
            throw new ProductNotFoundException("Quantity is too small for product with id " + productId);
        }

        return product;
    }
}
