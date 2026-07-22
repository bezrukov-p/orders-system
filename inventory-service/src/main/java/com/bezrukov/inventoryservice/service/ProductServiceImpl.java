package com.bezrukov.inventoryservice.service;

import com.bezrukov.inventoryservice.dto.ProductCreateRequest;
import com.bezrukov.inventoryservice.dto.ProductResponse;
import com.bezrukov.inventoryservice.entity.Product;
import com.bezrukov.inventoryservice.exception.ProductNotFoundException;
import com.bezrukov.inventoryservice.repository.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponse> findAll() {
        log.debug("find all products");
        return productRepository.findAll().stream().map(productMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public ProductResponse findById(Long id) {
        log.debug("find product by id: {}", id);
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ProductNotFoundException("Product not found"));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse create(ProductCreateRequest productCreateRequest) {
        log.debug("create product: {}", productCreateRequest.getName());
        Product product = productRepository.save(Product.builder()
                .name(productCreateRequest.getName())
                .price(productCreateRequest.getPrice())
                .quantity(productCreateRequest.getQuantity())
                .salePercent(productCreateRequest.getSalePercent())
                .build());
        log.info("product created: {}", product);
        return productMapper.toResponse(product);
    }

    @Override
    public void deleteById(Long id) {
        log.debug("delete product by id: {}", id);
        productRepository.deleteById(id);
        log.info("product deleted: {}", id);
    }

    @Override
    public Product getProductWithAvailability(Long productId, Long quantity) {
        log.debug("get product with availability: {}", productId);
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found with id " + productId)
        );
        if (product.getQuantity() < quantity) {
            log.warn("product with availability less than quantity: {}", productId);
            throw new ProductNotFoundException("Quantity is too small for product with id " + productId);
        }

        log.debug("product availability: productId={}. quantity={}", product.getId(), quantity);

        return product;
    }
}
