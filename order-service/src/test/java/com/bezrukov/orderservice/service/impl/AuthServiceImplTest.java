package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.dto.LoginResponse;
import com.bezrukov.orderservice.dto.RegisterRequest;
import com.bezrukov.orderservice.entity.RefreshToken;
import com.bezrukov.orderservice.entity.Role;
import com.bezrukov.orderservice.entity.Roles;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.exceptions.AuthenticationException;
import com.bezrukov.orderservice.service.JwtService;
import com.bezrukov.orderservice.service.RefreshTokenService;
import com.bezrukov.orderservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService: Аутентификация и управление токенами")
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    private User user;
    private Role roleUser;
    private RegisterRequest registerRequest;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        roleUser = Role.builder()
                .name(Roles.USER)
                .id(UUID.randomUUID())
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .username(USERNAME)
                .password(PASSWORD)
                .email("test@example.com")
                .roles(Set.of(roleUser))
                .build();

        registerRequest = RegisterRequest.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .email("test@example.com")
                .build();

        refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(REFRESH_TOKEN)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(60))
                .build();
    }

    @Nested
    @DisplayName("Регистрация пользователя")
    class RegistrationTests {

        @Test
        @DisplayName("При регистрации должен создаваться новый пользователь с ролью USER")
        void shouldRegisterUserSuccessfully() {
            when(userService.create(registerRequest, Set.of(Roles.USER)))
                    .thenReturn(user);

            User result = authService.register(registerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(USERNAME);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getRoles()).contains(roleUser);

            verify(userService, times(1))
                    .create(registerRequest, Set.of(Roles.USER));
        }

        @Test
        @DisplayName("Если userService.create() выбрасывает исключение, регистрация должна провалиться")
        void shouldThrowExceptionWhenRegistrationFails() {
            when(userService.create(registerRequest, Set.of(Roles.USER)))
                    .thenThrow(new RuntimeException("Username already taken"));

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Логин пользователя")
    class LoginTests {

        @Test
        @DisplayName("При успешном логине должны возвращаться access и refresh токены")
        void shouldLoginSuccessfully() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn(USERNAME);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(userService.getUser(USERNAME))
                    .thenReturn(Optional.of(user));

            when(jwtService.generateAccessToken(user))
                    .thenReturn(ACCESS_TOKEN);

            when(refreshTokenService.createRefreshToken(user))
                    .thenReturn(refreshToken);

            LoginResponse response = authService.login(USERNAME, PASSWORD);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.user()).isNotNull();
            assertThat(response.user().username()).isEqualTo(USERNAME);

            verify(authenticationManager, times(1))
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            verify(userService, times(1))
                    .getUser(USERNAME);

            verify(jwtService, times(1))
                    .generateAccessToken(user);

            verify(refreshTokenService, times(1))
                    .createRefreshToken(user);
        }

        @Test
        @DisplayName("При неверном пароле должно выбрасываться AuthenticationException")
        void shouldThrowExceptionWhenInvalidCredentials() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThatThrownBy(() -> authService.login(USERNAME, PASSWORD))
                    .isInstanceOf(AuthenticationException.class);

            verify(userService, never()).getUser(anyString());
            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Если пользователь не найден после аутентификации, должно выбрасываться AuthenticationException")
        void shouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.getName()).thenReturn(USERNAME);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(userService.getUser(USERNAME))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(USERNAME, PASSWORD))
                    .isInstanceOf(AuthenticationException.class);

            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Если username не существует, должно выбрасываться AuthenticationException")
        void shouldThrowExceptionWhenUsernameNotFound() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new UsernameNotFoundException("User not found"));

            assertThatThrownBy(() -> authService.login(USERNAME, PASSWORD))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("При неожиданной ошибке должно выбрасываться AuthenticationException")
        void shouldThrowExceptionOnUnexpectedError() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> authService.login(USERNAME, PASSWORD))
                    .isInstanceOf(AuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("Обновление токенов (Refresh Token)")
    class RefreshTokenTests {

        @Test
        @DisplayName("При валидном refresh токене должны генерироваться новые access и refresh токены")
        void shouldRefreshTokensSuccessfully() {
            when(refreshTokenService.getRefreshToken(REFRESH_TOKEN))
                    .thenReturn(Optional.of(refreshToken));

            when(refreshTokenService.validateRefreshToken(refreshToken))
                    .thenReturn(true);

            when(jwtService.generateAccessToken(user))
                    .thenReturn(ACCESS_TOKEN);

            RefreshToken newRefreshToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token("new-refresh-token-789")
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(60))
                    .build();

            when(refreshTokenService.createRefreshToken(user))
                    .thenReturn(newRefreshToken);

            LoginResponse response = authService.refreshToken(REFRESH_TOKEN);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token-789");
            assertThat(response.user()).isNotNull();
            assertThat(response.user().username()).isEqualTo(USERNAME);

            verify(refreshTokenService, times(1))
                    .getRefreshToken(REFRESH_TOKEN);

            verify(refreshTokenService, times(1))
                    .validateRefreshToken(refreshToken);

            verify(jwtService, times(1))
                    .generateAccessToken(user);

            verify(refreshTokenService, times(1))
                    .createRefreshToken(user);
        }

        @Test
        @DisplayName("Если refresh токен не найден, должно выбрасываться AuthenticationException")
        void shouldThrowExceptionWhenRefreshTokenNotFound() {
            when(refreshTokenService.getRefreshToken(REFRESH_TOKEN))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AuthenticationException.class);

            verify(refreshTokenService, never()).validateRefreshToken(any());
            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Если refresh токен невалиден (просрочен), должно выбрасываться AuthenticationException")
        void shouldThrowExceptionWhenRefreshTokenInvalid() {
            when(refreshTokenService.getRefreshToken(REFRESH_TOKEN))
                    .thenReturn(Optional.of(refreshToken));

            when(refreshTokenService.validateRefreshToken(refreshToken))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AuthenticationException.class);

            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Если пользователь не найден по refresh токену, должно выбрасываться AuthenticationException")
        void shouldThrowExceptionWhenUserNotFoundInRefreshToken() {
            RefreshToken refreshTokenWithoutUser = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(REFRESH_TOKEN)
                    .user(null)
                    .expiryDate(LocalDateTime.now().plusMinutes(60))
                    .build();

            when(refreshTokenService.getRefreshToken(REFRESH_TOKEN))
                    .thenReturn(Optional.of(refreshTokenWithoutUser));

            when(refreshTokenService.validateRefreshToken(refreshTokenWithoutUser))
                    .thenReturn(true);

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AuthenticationException.class);

            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }
    }
}