package com.bezrukov.orderservice.reposiroty;

import com.bezrukov.orderservice.entity.OutboxMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {
    long countByProcessedFalseAndFailedFalse();
    List<OutboxMessage> findByProcessedFalseAndFailedFalseOrderByCreatedAtAsc(Limit limit);
}
