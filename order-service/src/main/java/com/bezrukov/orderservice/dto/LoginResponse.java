package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с данными аутентификации")
public record LoginResponse(

        @Schema(
                description = "JWT токен доступа",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String token,

        @Schema(
                description = "Refresh токен для обновления доступа",
                example = "d7f8a9b0-c1d2-4e5f-8a9b-0c1d2e3f4a5b",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String refreshToken,

        @Schema(
                description = "Информация о пользователе",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UserDto user
) {}
