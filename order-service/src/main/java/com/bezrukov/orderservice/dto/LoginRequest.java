package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на аутентификацию (регистрация/вход)")
public record LoginRequest(

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
        String password
) {}
