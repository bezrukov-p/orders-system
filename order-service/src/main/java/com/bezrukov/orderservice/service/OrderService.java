package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.dto.OrderItemRequest;
import com.bezrukov.orderservice.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    @Operation(
            summary = "Создать заказ",
            description = "Создает заказ на основе списка товаров и ID пользователя"
    )
    OrderResponse createOrder(
            @Parameter(description = "Список товаров в заказе", required = true)
            List<OrderItemRequest> itemsRequest,

            @Parameter(description = "ID пользователя", required = true)
            UUID userId
    );
}
