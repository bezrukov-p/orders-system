package com.bezrukov.common.event;

import com.bezrukov.common.dto.ReservedItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {
    private UUID orderId;
    private String idempotencyKey;
    private boolean success;
    private String message;
    private List<ReservedItemDto> items;
}
