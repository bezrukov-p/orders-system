package com.bezrukov.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Базовый ответ с ошибкой")
public class ErrorResponse {

    @Schema(
            description = "HTTP статус код ошибки"
    )
    private int status;

    @Schema(
            description = "Сообщение об ошибке"
    )
    private String message;

    @Schema(
            description = "Время возникновения ошибки в формате ISO",
            example = "2024-01-15T10:30:00.123",
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    )
    private LocalDateTime timestamp;

    @Schema(
            description = "URL пути запроса, на котором возникла ошибка",
            example = "/api/users",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String path;

    @Schema(
            description = "Список детальных ошибок (для множественных ошибок валидации)",
            nullable = true
    )
    private List<String> errors;

    @Schema(
            description = "Код ошибки для программной обработки",
            allowableValues = {
                    "USER_NOT_FOUND",
                    "ORDER_NOT_FOUND",
                    "USER_ALREADY_EXISTS",
                    "INVALID_REFRESH_TOKEN",
                    "INVALID_STATUS",
                    "ACCESS_DENIED",
                    "AUTHENTICATION_FAILED",
                    "VALIDATION_FAILED"
            },
            nullable = true
    )
    private String errorCode;

    @Schema(
            description = "Тип ошибки для классификации",
            nullable = true,
            hidden = true
    )
    private String errorType;
}
