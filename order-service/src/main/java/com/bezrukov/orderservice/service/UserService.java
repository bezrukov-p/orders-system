package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.dto.RegisterRequest;
import com.bezrukov.orderservice.entity.User;

import java.util.Optional;
import java.util.Set;

public interface UserService {
    User create(RegisterRequest registerRequest, Set<String> roles);

    Optional<User> getUser(String username);
}
