package com.bezrukov.orderservice.dto;

public record ProductAvailabilityResponse(
        Long productId,
        Long quantity
) {
}
