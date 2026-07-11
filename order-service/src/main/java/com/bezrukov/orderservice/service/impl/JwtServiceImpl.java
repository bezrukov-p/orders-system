package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.entity.Role;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
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

    private final SecretKey jwtAccessKey;
    private final Duration jwtAccessDuration;

    public JwtServiceImpl(
            SecretKey jwtAccessKey,
            @Value("${jwt.access.duration}") Duration jwtAccessDuration) {
        this.jwtAccessKey = jwtAccessKey;
        this.jwtAccessDuration = jwtAccessDuration;
    }

    @Override
    public String extractUsername(String jwt) {
        return Jwts.parser()
                .verifyWith(jwtAccessKey)
                .build()
                .parseSignedClaims(jwt)
                .getPayload()
                .getSubject();
    }

    @Override
    public boolean isAccessTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtAccessKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (ExpiredJwtException e) {
            log.debug("Access token expired: {}", e.getMessage());
            return false;

        } catch (SignatureException e) {
            log.warn("Invalid access token signature: {}", e.getMessage());
            return false;

        } catch (MalformedJwtException e) {
            log.warn("Malformed access token: {}", e.getMessage());
            return false;

        } catch (Exception e) {
            log.warn("Invalid access token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String generateAccessToken(User user) {
        String accessToken = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .claim("userId", user.getId())
                .claim("roles", user.getRoles().stream()
                        .map(Role::getName).toList())
                .expiration(Date.from(Instant.now().plus(jwtAccessDuration)))
                .signWith(jwtAccessKey)
                .compact();
        log.info("Created access token for {}", user.getUsername());
        return accessToken;
    }
}
