package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.dto.LoginResponse;
import com.bezrukov.orderservice.dto.RegisterRequest;
import com.bezrukov.orderservice.entity.RefreshToken;
import com.bezrukov.orderservice.entity.Roles;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.exceptions.AuthenticationException;
import com.bezrukov.orderservice.service.AuthService;
import com.bezrukov.orderservice.service.JwtService;
import com.bezrukov.orderservice.service.RefreshTokenService;
import com.bezrukov.orderservice.service.UserService;
import com.bezrukov.orderservice.utils.MapperDto;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Реализация аутентификации: регистрация, логин, обновление токенов.
 *
 * <p>Использует Spring Security для проверки пароля, JWT для access-токена
 * и отдельную таблицу refresh-токенов для долгоживущих сессий.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрирует нового пользователя с ролью USER.
     */
    @Override
    public User register(RegisterRequest registerRequest) {
        return userService.create(registerRequest, Set.of(Roles.USER));
    }

    /**
     * Проверяет логин/пароль и выдаёт пару access + refresh токенов.
     *
     * <p>При любой ошибке аутентификации возвращает обезличенное
     * {@link AuthenticationException} — чтобы не раскрывать, существует
     * ли пользователь с таким именем.
     */
    @WithSpan("auth.login")
    @Override
    public LoginResponse login(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            User user = userService.getUser(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException(
                            String.format("User with username '%s' not found", authentication.getName())
                    ));

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

            log.info("User logged in successfully: {}", username);

            return new LoginResponse(
                    accessToken,
                    refreshToken,
                    MapperDto.userToDto(user)
            );

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username: {}", username);
            throw new AuthenticationException("Invalid credentials");
        } catch (UsernameNotFoundException e) {
            log.warn("Login attempt with non-existent username: {}", username);
            throw new AuthenticationException("Invalid credentials");
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", username, e);
            throw new AuthenticationException("Authentication failed");
        }
    }

    /**
     * Обновляет access-токен по действующему refresh-токену.
     * Старый refresh-токен инвалидируется, выдается новый.
     *
     * @return новый {@link LoginResponse}
     */
    @Override
    public @Nullable LoginResponse refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenService.getRefreshToken(token).orElseThrow(
                () -> new AuthenticationException("invalid refresh token")
        );
        if (!refreshTokenService.validateRefreshToken(refreshToken)) {
            throw new AuthenticationException("invalid refresh token");
        }

        User user = refreshToken.getUser();
        if (user == null) {
            throw new AuthenticationException("User not found");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user).getToken();
        log.info("Access and refresh token generated successfully for {}", user.getUsername());

        return new LoginResponse(newAccessToken, newRefreshToken, MapperDto.userToDto(user));
    }
}
