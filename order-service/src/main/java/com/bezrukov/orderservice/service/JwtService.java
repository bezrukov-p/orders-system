package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.entity.User;

public interface JwtService {
    String generateAccessToken(User user);
}
