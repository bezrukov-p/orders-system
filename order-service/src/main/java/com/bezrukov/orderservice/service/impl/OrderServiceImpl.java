package com.bezrukov.orderservice.service.impl;

import com.bezrukov.common.grpc.InventoryServiceGrpc;
import com.bezrukov.common.grpc.ProductRequest;
import com.bezrukov.common.grpc.ProductResponse;
import com.bezrukov.orderservice.dto.ProductAvailabilityResponse;
import com.bezrukov.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    @Override
    public ProductAvailabilityResponse checkAvailability(Long productId) {
        ProductResponse response = inventoryStub.checkAvailability(ProductRequest.newBuilder()
                .setProductId(productId)
                .setQuantity(1L)
                .build());
        return new ProductAvailabilityResponse(response.getId(), response.getQuantity());
    }
}
