package com.bezrukov.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Данные пользователя")
public record UserDto(

        @Schema(
                description = "Уникальный идентификатор пользователя",
                example = "123e4567-e89b-12d3-a456-426614174000",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Имя пользователя",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String username,

        @Schema(
                description = "Email пользователя",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String email,

        @Schema(
                description = "Роль пользователя в системе",
                example = "ROLE_USER",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Set<String> roles
) {}
