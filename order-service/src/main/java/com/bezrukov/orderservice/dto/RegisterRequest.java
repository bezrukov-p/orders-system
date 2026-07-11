package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterRequest(
        @Schema(
                description = "Имя пользователя для входа в систему",
                example = "john_doe",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String username,

        @Schema(
                description = "Пароль пользователя",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String password,

        @Schema(
                description = "email пользователя",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String email
) {}
