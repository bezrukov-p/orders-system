package com.bezrukov.inventoryservice.repository;

import com.bezrukov.inventoryservice.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    boolean existsByKey(String key);
}
