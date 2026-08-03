package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.dto.OrderRequest;
import com.bezrukov.orderservice.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.UUID;

public interface OrderService {

    @Operation(
            summary = "Создать заказ",
            description = "Создает заказ на основе списка товаров и ID пользователя"
    )
    OrderResponse createOrder(
            @Parameter(description = "Запрос на создание заказа", required = true)
            OrderRequest orderRequest,

            @Parameter(description = "ID пользователя", required = true)
            UUID userId
    );
}
