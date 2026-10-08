package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на обновление токена доступа")
public record RefreshTokenRequest(

        @Schema(
                description = "Refresh токен для получения новой пары токенов",
                example = "d7f8a9b0-c1d2-4e5f-8a9b-0c1d2e3f4a5b",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 36,
                maxLength = 36,
                pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
        )
        String refreshToken
) {}
