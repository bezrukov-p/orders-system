package com.bezrukov.inventoryservice.service;

import com.bezrukov.common.dto.OrderItemDto;
import com.bezrukov.common.dto.ReservedItemDto;
import com.bezrukov.common.event.ReserveStockCommand;
import com.bezrukov.common.event.StockReservedEvent;
import com.bezrukov.inventoryservice.dto.InsufficientStockItem;
import com.bezrukov.inventoryservice.entity.IdempotencyKey;
import com.bezrukov.inventoryservice.entity.Product;
import com.bezrukov.inventoryservice.exception.InsufficientStockException;
import com.bezrukov.inventoryservice.exception.ProductNotFoundException;
import com.bezrukov.inventoryservice.kafka.InventoryEventProducer;
import com.bezrukov.inventoryservice.repository.IdempotencyKeyRepository;
import com.bezrukov.inventoryservice.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockReservationService {
    private final ProductRepository productRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public StockReservedEvent reserveStock(ReserveStockCommand command) {
        //TODO названия зарефакторить
        UUID orderId = command.getOrderId();
        String idempotencyKey = command.getIdempotencyKey();
        List<OrderItemDto> orderItems = command.getItems();

        if (idempotencyKeyRepository.existsByKey(idempotencyKey)) {
            log.warn("Duplicate command orderId: {}. idempotencyKey: {}", orderId, idempotencyKey);
            return StockReservedEvent.builder()
                    .orderId(orderId)
                    .idempotencyKey(idempotencyKey)
                    .success(false)
                    .message("Duplicate command orderId: " + orderId)
                    .build();
        }

        List<Long> productIds = orderItems.stream().map(OrderItemDto::getProductId).toList();
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            Set<Long> foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
            List<Long> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new ProductNotFoundException("products not found: " + missingIds);
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<InsufficientStockItem> insufficientItems = new ArrayList<>();
        List<ReservedItemDto> reservedItems = new ArrayList<>();
        for (var item : orderItems) {
            Product product = productMap.get(item.getProductId());

            if (product.getQuantity() < item.getQuantity()) {
                insufficientItems.add(InsufficientStockItem.builder()
                        .productId(product.getId())
                        .availableQuantity(product.getQuantity())
                        .requestedQuantity(item.getQuantity())
                        .build());
            }

            product.setQuantity(product.getQuantity() - item.getQuantity());

            reservedItems.add(ReservedItemDto.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .quantity(item.getQuantity())
                    .salePercent(product.getSalePercent())
                    .totalPrice(product.getPrice() * (1 - product.getSalePercent()))
                    .build());
        }

        if (!insufficientItems.isEmpty()) {
            throw new InsufficientStockException("Insufficient stock for products: " + insufficientItems); //TODO перехватывать exception?
        }

        productRepository.saveAll(products);

        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .key(idempotencyKey)
                .build());

        log.info("Stock reserved: orderId={}", orderId);
        return StockReservedEvent.builder()
                .orderId(orderId)
                .items(reservedItems)
                .idempotencyKey(idempotencyKey)
                .success(true)
                .message("Stock reserved successfully")
                .build();
    }
}
