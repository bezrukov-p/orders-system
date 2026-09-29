package com.bezrukov.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private List<OrderItemEvent> items;
    private Double totalPrice;
    private LocalDateTime createdAt;
}
