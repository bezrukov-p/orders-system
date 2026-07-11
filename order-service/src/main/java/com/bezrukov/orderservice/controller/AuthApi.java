package com.bezrukov.orderservice.controller;

import com.bezrukov.orderservice.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Аутентификация", description = "API для регистрации, входа и управления токенами")
public interface AuthApi {

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя в системе и возвращает его данные"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса (ошибка валидации)",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким именем или email уже существует",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    ResponseEntity<UserDto> register(
            @Parameter(description = "Данные для регистрации", required = true,
                    schema = @Schema(implementation = RegisterRequest.class))
            @RequestBody RegisterRequest registerRequest
    );

    @Operation(
            summary = "Вход в систему",
            description = "Аутентификация пользователя и получение JWT токенов"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса (ошибка валидации)",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверное имя пользователя или пароль",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(
            @Parameter(description = "Учетные данные пользователя", required = true,
                    schema = @Schema(implementation = LoginRequest.class))
            @RequestBody LoginRequest loginRequest
    );

    @Operation(
            summary = "Обновление токена доступа",
            description = "Получение новой пары токенов (access и refresh) по refresh токену"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токены успешно обновлены",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный refresh токен (ошибка валидации)",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh токен истек или недействителен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    ResponseEntity<LoginResponse> refreshToken(
            @Parameter(description = "Refresh токен для обновления", required = true,
                    schema = @Schema(implementation = RefreshTokenRequest.class))
            @RequestBody RefreshTokenRequest refreshTokenRequest
    );
}
