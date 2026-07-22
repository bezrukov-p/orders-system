package com.bezrukov.orderservice.service.impl;

import com.bezrukov.common.grpc.InventoryServiceGrpc;
import com.bezrukov.common.grpc.ProductRequest;
import com.bezrukov.common.grpc.ProductResponse;
import com.bezrukov.orderservice.dto.OrderItemRequest;
import com.bezrukov.orderservice.dto.OrderItemResponse;
import com.bezrukov.orderservice.dto.OrderResponse;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.OrderItem;
import com.bezrukov.orderservice.entity.Status;
import com.bezrukov.orderservice.event.OrderEventPublisher;
import com.bezrukov.orderservice.exceptions.ProductNotAvailableException;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import com.bezrukov.orderservice.service.OrderService;
import com.bezrukov.orderservice.service.UserService;
import com.bezrukov.orderservice.utils.OrderEventMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderEventMapper orderEventMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(List<OrderItemRequest> itemsRequest, UUID userId) {
        List<ProductResponse> productsResult = checkProductStock(itemsRequest);

        if (productsResult.stream().anyMatch(Objects::isNull)) {
            log.error("some product was not found");
            throw new ProductNotAvailableException("some product was not found");
        }

        List<ProductResponse> unavailableDetails = getOrderItemRequests(itemsRequest, productsResult);
        if (!unavailableDetails.isEmpty()) {
            String errorMessage = "Some products are not available in requested quantities: "
                    + String.join("; ", unavailableDetails.stream()
                    .map(product -> String.valueOf(product.getId())).toList()
            );
            log.error(errorMessage);
            throw new ProductNotAvailableException(errorMessage);
        }

        Map<Long, Double> productPriceWithDiscount = productsResult.stream().collect(Collectors.toMap(
                ProductResponse::getId,
                product -> product.getPrice() * (1 - product.getSalePercent() / 100.0)
        ));
        Double totalPrice = calculateTotalPrice(itemsRequest, productPriceWithDiscount);

        Order orderToSave = Order.builder()
                .status(Status.CREATED)
                .user(userService.getReferenceById(userId))
                .totalPrice(totalPrice)
                .createdAt(LocalDateTime.now())
                .build();
        List<OrderItem> itemsToSave = productsResult.stream()
                .map(product -> OrderItem.builder()
                        .order(orderToSave)
                        .productId(product.getId())
                        .quantity(product.getQuantity())
                        .price(product.getPrice())
                        .salePercent(product.getSalePercent())
                        .build()
                ).toList();
        orderToSave.setItems(itemsToSave);
        Order order = orderRepository.save(orderToSave);

        orderEventPublisher.publishOrderCreated(orderEventMapper.toOrderCreatedEvent(order));

        //TODO возвращать название продукта
        return new OrderResponse(order.getId(), userId, order.getStatus().name(),
                order.getTotalPrice(), order.getCreatedAt(), order.getItems().stream().map(
                        orderItem -> new OrderItemResponse(orderItem.getId(), orderItem.getProductId(),
                                orderItem.getProductId().toString(), orderItem.getQuantity(), orderItem.getPrice(),
                                orderItem.getSalePercent())).toList());
    }

    private List<ProductResponse> getOrderItemRequests(
            List<OrderItemRequest> orderItemsRequest,
            List<ProductResponse> productsResult
    ) {
        List<ProductResponse> unavailableProducts = new ArrayList<>();
        for (int i = 0; i < productsResult.size(); i++) {
            OrderItemRequest productRequest = orderItemsRequest.get(i);
            ProductResponse productResponse = productsResult.get(i);

            long requestedQuantity = productRequest.getQuantity();
            long availableQuantity = productResponse.getQuantity();
            if (availableQuantity < requestedQuantity) {
                unavailableProducts.add(productResponse);
            }
        }
        return unavailableProducts;
    }

    private List<ProductResponse> checkProductStock(List<OrderItemRequest> orderItemsRequest) {
        List<CompletableFuture<ProductResponse>> inventoryResponses = orderItemsRequest.stream().map(item -> {
            ProductRequest request = ProductRequest.newBuilder()
                    .setProductId(item.getProductId())
                    .setQuantity(item.getQuantity())
                    .build();

            return CompletableFuture.supplyAsync(() -> inventoryStub.checkAvailability(request));
        }).toList();

        try {
            CompletableFuture.allOf(inventoryResponses.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Timeout while waiting for available products to complete");
            throw new RuntimeException("Timeout while waiting for available products to complete");
        } catch (Exception e) {
            log.error("Error while waiting for available products to complete");
            throw new RuntimeException("Failed to wait for available products to complete " + e.getMessage());
        }
        return inventoryResponses
                .stream().map(CompletableFuture::join).toList();
    }

    private Double calculateTotalPrice(List<OrderItemRequest> items, Map<Long, Double> itemsPrice) {
        AtomicReference<Double> totalPrice = new AtomicReference<>(0.0);
        items.forEach(product -> totalPrice
                .updateAndGet(v -> v + itemsPrice.getOrDefault(product.getProductId(), 0.0)));
        return totalPrice.get();
    }
}
