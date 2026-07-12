package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.dto.ProductAvailabilityResponse;

public interface OrderService {
    ProductAvailabilityResponse checkAvailability(Long productId);
}
