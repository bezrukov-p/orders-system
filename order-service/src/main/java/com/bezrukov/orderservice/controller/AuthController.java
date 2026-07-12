package com.bezrukov.orderservice.controller;

import com.bezrukov.orderservice.dto.LoginRequest;
import com.bezrukov.orderservice.dto.LoginResponse;
import com.bezrukov.orderservice.dto.RefreshTokenRequest;
import com.bezrukov.orderservice.dto.RegisterRequest;
import com.bezrukov.orderservice.dto.UserDto;
import com.bezrukov.orderservice.service.AuthService;
import com.bezrukov.orderservice.utils.MapperDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/auth")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService authService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(RegisterRequest registerRequest) {
        log.info("Registering user: {}", registerRequest.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                MapperDto.userToDto(authService.register(registerRequest))
        );
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        log.info("Login request: {}", loginRequest.username());
        return ResponseEntity.ok(authService.login(loginRequest.username(), loginRequest.password()));
    }

    @Override
    @PostMapping("/refreshtoken")
    public ResponseEntity<LoginResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Refresh token request: {}", refreshTokenRequest.refreshToken());
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest.refreshToken()));
    }
}
