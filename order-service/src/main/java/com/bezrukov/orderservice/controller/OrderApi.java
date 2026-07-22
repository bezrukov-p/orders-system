package com.bezrukov.orderservice.controller;

import com.bezrukov.orderservice.dto.OrderRequest;
import com.bezrukov.orderservice.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "Управление заказами", description = "API для создания и управления заказами")
public interface OrderApi {

    @Operation(
            summary = "Создать новый заказ",
            description = "Создает заказ с несколькими товарами. Требуется JWT-аутентификация.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заказ успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Товар не найден"),
            @ApiResponse(responseCode = "409", description = "Товар недоступен в запрошенном количестве")
    })
    ResponseEntity<OrderResponse> createOrder(
            @Parameter(
                    description = "JWT токен пользователя",
                    required = true,
                    in = ParameterIn.HEADER
            )
            Jwt jwt,

            @Parameter(
                    description = "Данные для создания заказа",
                    required = true
            )
            OrderRequest orderRequest
    );
}
