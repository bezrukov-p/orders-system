package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.entity.Role;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final SecretKey jwtAccessSecretKey;
    private final Duration jwtAccessDuration;

    public JwtServiceImpl(
            SecretKey jwtAccessSecretKey,
            @Value("${jwt.access.duration}") Duration jwtAccessDuration) {
        this.jwtAccessSecretKey = jwtAccessSecretKey;
        this.jwtAccessDuration = jwtAccessDuration;
    }

    @WithSpan("jwt.access")
    @Override
    public String generateAccessToken(User user) {
        String accessToken = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .claim("userId", user.getId())
                .claim("roles", user.getRoles().stream()
                        .map(Role::getName).toList())
                .expiration(Date.from(Instant.now().plus(jwtAccessDuration)))
                .signWith(jwtAccessSecretKey)
                .compact();
        log.info("Created access token for {}", user.getUsername());
        return accessToken;
    }
}
