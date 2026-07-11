package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.entity.User;

public interface JwtService {

    String extractUsername(String jwt);

    boolean isAccessTokenValid(String jwt);

    String generateAccessToken(User user);
}
