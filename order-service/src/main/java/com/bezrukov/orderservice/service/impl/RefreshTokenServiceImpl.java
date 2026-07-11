package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.entity.RefreshToken;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.reposiroty.RefreshTokenRepository;
import com.bezrukov.orderservice.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration jwtRefreshDuration;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository
            ,@Value("${jwt.refresh.duration}") Duration jwtRefreshDuration) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtRefreshDuration = jwtRefreshDuration;
    }

    @Override
    public RefreshToken createRefreshToken(User user) {
        List<RefreshToken> oldTokens = refreshTokenRepository.findByUser(user);
        oldTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(oldTokens);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plus(jwtRefreshDuration))
                .user(user)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for {}", user.getUsername());
        return refreshToken;
    }

    @Override
    public boolean validateRefreshToken(RefreshToken token) {
        return token != null &&
                !token.isRevoked() &&
                token.getExpiryDate().isAfter(LocalDateTime.now());
    }

    @Override
    public Optional<RefreshToken> getRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}
