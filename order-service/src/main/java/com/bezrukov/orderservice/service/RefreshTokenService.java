package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.entity.RefreshToken;
import com.bezrukov.orderservice.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);

    boolean validateRefreshToken(RefreshToken token);

    Optional<RefreshToken> getRefreshToken(String token);
}
