package com.bezrukov.orderservice.controller;

import com.bezrukov.orderservice.dto.OrderRequest;
import com.bezrukov.orderservice.dto.OrderResponse;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.service.OrderService;
import com.bezrukov.orderservice.utils.MapperDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
@Slf4j
public class OrderController implements  OrderApi {
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid OrderRequest orderRequest) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaim("userId")));

        Order order = orderService.createOrder(orderRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(MapperDto.toOrderResponse(order, userId));
    }

    @Override
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaim("userId")));

        Order order = orderService.getOrder(orderId, userId);
        return ResponseEntity.ok(MapperDto.toOrderResponse(order, userId));
    }
}
