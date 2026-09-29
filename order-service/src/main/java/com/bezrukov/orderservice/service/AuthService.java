package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.dto.LoginResponse;
import com.bezrukov.orderservice.dto.RegisterRequest;
import com.bezrukov.orderservice.entity.User;
import org.jspecify.annotations.Nullable;

public interface AuthService {
    User register(RegisterRequest registerRequest);

    @Nullable LoginResponse login(String username, String password);

    @Nullable LoginResponse refreshToken(String s);
}
