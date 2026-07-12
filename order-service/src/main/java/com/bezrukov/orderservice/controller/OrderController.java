package com.bezrukov.orderservice.controller;

import com.bezrukov.orderservice.dto.ProductAvailabilityResponse;
import com.bezrukov.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/check/{productId}")
    public ResponseEntity<ProductAvailabilityResponse> checkAvailability(@PathVariable Long productId) {
        ProductAvailabilityResponse response = orderService.checkAvailability(productId);
        return ResponseEntity.ok(response);
    }
}
