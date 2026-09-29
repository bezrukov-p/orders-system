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

import java.util.UUID;

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

    @Operation(
            summary = "Получить заказ по ID",
            description = "Возвращает информацию о заказе, включая текущий статус (PENDING, CONFIRMED, CANCELLED). " +
                    "Доступен только владельцу заказа. Требуется JWT-аутентификация.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ найден"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к чужому заказу"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    ResponseEntity<OrderResponse> getOrder(
            @Parameter(
                    description = "JWT токен пользователя",
                    required = true,
                    in = ParameterIn.HEADER
            )
            Jwt jwt,

            @Parameter(
                    description = "ID заказа",
                    required = true,
                    in = ParameterIn.PATH,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            UUID orderId
    );
}
