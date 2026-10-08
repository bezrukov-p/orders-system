package com.bezrukov.orderservice.repository;

import com.bezrukov.orderservice.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {
    long countByProcessedFalseAndFailedFalse();

    @Query(value = """
    SELECT * FROM outbox_messages
    WHERE processed = false AND failed = false
    ORDER BY created_at
    LIMIT :limit
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<OutboxMessage> lockPendingBatch(@Param("limit") int limit);
}
