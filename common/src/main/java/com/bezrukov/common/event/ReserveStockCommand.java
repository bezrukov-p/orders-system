package com.bezrukov.common.event;

import com.bezrukov.common.dto.OrderItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReserveStockCommand {
    private UUID orderId;
    private String idempotencyKey;
    private List<OrderItemDto> items;
}
