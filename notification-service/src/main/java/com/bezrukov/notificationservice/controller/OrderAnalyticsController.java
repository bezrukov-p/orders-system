package com.bezrukov.notificationservice.controller;

import com.bezrukov.notificationservice.dto.OrderResponse;
import com.bezrukov.notificationservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Заказы (аналитика)", description = "Read-only API для аналитики заказов")
public class OrderAnalyticsController {

    private final OrderService orderService;

    @GetMapping("/all")
    @Operation(
            summary = "Получить все заказы",
            description = "Возвращает список всех заказов из базы данных"
    )
    @ApiResponse(responseCode = "200", description = "Список заказов успешно получен")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Operation(
            summary = "Получить заказ по orderId",
            description = "Возвращает заказ и все его позиции по ID из Order Service"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ найден"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    public ResponseEntity<OrderResponse> getOrderByOrderId(
            @Parameter(description = "ID заказа из Order Service", required = true)
            @PathVariable UUID orderId
    ) {
        OrderResponse order = orderService.getOrderByOrderId(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Получить заказы пользователя",
            description = "Возвращает все заказы указанного пользователя"
    )
    @ApiResponse(responseCode = "200", description = "Список заказов пользователя получен")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable UUID userId
    ) {
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
}
