package com.bezrukov.orderservice.reposiroty;

import com.bezrukov.orderservice.entity.RefreshToken;
import com.bezrukov.orderservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByToken(String token);
}
