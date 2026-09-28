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
import com.bezrukov.inventoryservice.repository.IdempotencyKeyRepository;
import com.bezrukov.inventoryservice.repository.ProductRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
    @WithSpan("reserve.stock")
    public StockReservedEvent reserveStock(ReserveStockCommand command) {
        UUID orderId = command.getOrderId();
        String idempotencyKey = command.getIdempotencyKey();
        List<OrderItemDto> orderItems = command.getItems();

        if (idempotencyKeyRepository.existsByKey(idempotencyKey)) {
            log.warn("Duplicate command orderId: {}. idempotencyKey: {}", orderId, idempotencyKey);
            return duplicateResponse(orderId, idempotencyKey);
        }

        Map<Long, Product> productMap = loadProductsOrThrow(orderItems);

        List<InsufficientStockItem> insufficientItems = findInsufficientItems(orderItems, productMap);

        if (!insufficientItems.isEmpty()) {
            throw new InsufficientStockException("Insufficient stock for products: " + insufficientItems);
        }

        List<ReservedItemDto> reservedItems = reserveAndBuildItems(orderItems, productMap);
        productRepository.saveAll(productMap.values());

        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .key(idempotencyKey)
                .build());

        log.info("Stock reserved: orderId={}", orderId);
        return successResponse(orderId, idempotencyKey, reservedItems);
    }

    private Map<Long, Product> loadProductsOrThrow(List<OrderItemDto> orderItems) {
        List<Long> productIds = orderItems.stream()
                .map(OrderItemDto::getProductId)
                .sorted() // для избежания race condition
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            Set<Long> foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
            List<Long> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new ProductNotFoundException("products not found: " + missingIds);
        }

        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private List<InsufficientStockItem> findInsufficientItems(
            List<OrderItemDto> orderItems, Map<Long, Product> productMap) {

        List<InsufficientStockItem> insufficientItems = new ArrayList<>();

        for (OrderItemDto item : orderItems) {
            Product product = productMap.get(item.getProductId());
            if (product.getQuantity() < item.getQuantity()) {
                insufficientItems.add(InsufficientStockItem.builder()
                        .productId(product.getId())
                        .availableQuantity(product.getQuantity())
                        .requestedQuantity(item.getQuantity())
                        .build());
            }
        }

        return insufficientItems;
    }

    private List<ReservedItemDto> reserveAndBuildItems(
            List<OrderItemDto> orderItems, Map<Long, Product> productMap) {

        List<ReservedItemDto> reservedItems = new ArrayList<>(orderItems.size());

        for (OrderItemDto item : orderItems) {
            Product product = productMap.get(item.getProductId());
            product.setQuantity(product.getQuantity() - item.getQuantity());

            reservedItems.add(ReservedItemDto.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .quantity(item.getQuantity())
                    .salePercent(product.getSalePercent())
                    .totalPrice(calculateTotalPrice(item, product))
                    .build());
        }

        return reservedItems;
    }

    private double calculateTotalPrice(OrderItemDto item, Product product) {
        double discountMultiplier = (100 - product.getSalePercent()) / 100.0;
        return item.getQuantity() * product.getPrice() * discountMultiplier;
    }

    private StockReservedEvent duplicateResponse(UUID orderId, String idempotencyKey) {
        return StockReservedEvent.builder()
                .orderId(orderId)
                .idempotencyKey(idempotencyKey)
                .success(false)
                .message("Duplicate command orderId: " + orderId)
                .build();
    }

    private StockReservedEvent successResponse(UUID orderId, String idempotencyKey,
                                               List<ReservedItemDto> reservedItems) {
        return StockReservedEvent.builder()
                .orderId(orderId)
                .items(reservedItems)
                .idempotencyKey(idempotencyKey)
                .success(true)
                .message("Stock reserved successfully")
                .build();
    }
}
