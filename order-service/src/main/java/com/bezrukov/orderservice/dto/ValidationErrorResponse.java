package com.bezrukov.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ответ с ошибками валидации полей")
@AllArgsConstructor
public class ValidationErrorResponse {

    @Schema(
            description = "HTTP статус код ошибки",
            example = "400",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int status;

    @Schema(
            description = "Общее сообщение об ошибке",
            example = "Validation failed",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;

    @Schema(
            description = "Время возникновения ошибки",
            example = "2024-01-15T10:30:00.123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime timestamp;

    @Schema(
            description = "Детальные ошибки по каждому полю",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Map<String, String> fieldErrors;
}
