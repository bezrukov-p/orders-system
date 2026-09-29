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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Резервирование товаров на складе по команде от order-service.
 *
 * <p><b>Идемпотентность:</b> повторная команда с тем же {@code idempotencyKey}
 * не приводит к повторному списанию.
 *
 * <p><b>Защита от race condition:</b> остатки проверяются дважды — сначала
 * в памяти (для читаемого сообщения), затем атомарно в SQL через
 * {@code UPDATE ... WHERE quantity >= ?}.
 *
 * <p><b>Защита от deadlock:</b> позиции сортируются по {@code productId}
 * перед списанием — все транзакции блокируют строки в одном порядке.
 */
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

        //sorted для избежания deadlock
        List<OrderItemDto> sortedItems = orderItems.stream()
                .sorted(Comparator.comparing(OrderItemDto::getProductId))
                .toList();

        List<ReservedItemDto> reservedItems = new ArrayList<>(sortedItems.size());

        for (OrderItemDto item : sortedItems) {
            int updated = productRepository.tryReserve(item.getProductId(), item.getQuantity());

            if (updated == 0) {
                Product product = productMap.get(item.getProductId());
                log.error("Race condition: productId={}, requested={}, available={}",
                        item.getProductId(), item.getQuantity(), product.getQuantity());
                //error для удобного нахождения в логах

                throw new InsufficientStockException(String.format(
                        "Race condition: productId=%d, requested=%d, available=%d",
                        item.getProductId(), item.getQuantity(), product.getQuantity()
                ));
            }

            Product product = productMap.get(item.getProductId());
            reservedItems.add(buildReservedItem(item, product));
        }

        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .key(idempotencyKey)
                .build());

        log.info("Stock reserved: orderId={}", orderId);
        return successResponse(orderId, idempotencyKey, reservedItems);
    }

    private Map<Long, Product> loadProductsOrThrow(List<OrderItemDto> orderItems) {
        List<Long> productIds = orderItems.stream()
                .map(OrderItemDto::getProductId)
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

    private ReservedItemDto buildReservedItem(OrderItemDto item, Product product) {
        return ReservedItemDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .quantity(item.getQuantity())
                .salePercent(product.getSalePercent())
                .totalPrice(calculateTotalPrice(item, product))
                .build();
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
