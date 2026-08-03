package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.dto.OrderItemRequest;
import com.bezrukov.orderservice.dto.OrderItemResponse;
import com.bezrukov.orderservice.dto.OrderRequest;
import com.bezrukov.orderservice.dto.OrderResponse;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.Status;
import com.bezrukov.orderservice.kafka.OrderCommandProducer;
import com.bezrukov.orderservice.reposiroty.OrderRepository;
import com.bezrukov.orderservice.service.OrderService;
import com.bezrukov.orderservice.service.UserService;
import com.bezrukov.common.dto.OrderItemDto;
import com.bezrukov.common.event.ReserveStockCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    //private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;
    private final OrderRepository orderRepository;
    private final OrderCommandProducer orderCommandProducer;
    private final UserService userService;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, UUID userId) {
        String idempotencyKey = orderRequest.getIdempotencyKey();
        List<OrderItemRequest> itemsRequest = orderRequest.getItems();

        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            log.info("Idempotent request detected: returning existing order {}", existingOrder.get().getId());
            return toOrderResponse(existingOrder.get(), userId);
        }

        Order order = Order.builder()
                .idempotencyKey(idempotencyKey)
                .user(userService.getReferenceById(userId))
                .status(Status.PENDING)  //TODO чем заполнять .items
                .totalPrice(0.0)
                .build();
        order = orderRepository.save(order);

        ReserveStockCommand reserveCommand = ReserveStockCommand.builder()
                .orderId(order.getId())
                .idempotencyKey(order.getIdempotencyKey())
                .items(itemsRequest.stream()
                        .map(item -> OrderItemDto.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();
        orderCommandProducer.sendReserveStockCommand(reserveCommand);

        log.info("Order created with status PENDING: orderId={}", order.getId());

        return toOrderResponse(order, userId);
    }

    private OrderResponse toOrderResponse(Order order, UUID userId) {
        return new OrderResponse(
                order.getId(),
                userId,
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                order.getItems().stream().map(orderItem -> new OrderItemResponse(
                        orderItem.getId(),
                        orderItem.getProductId(),
                        orderItem.getProductId().toString(),   //TODO сделать наименование товара
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getSalePercent()
                )).toList()
        );
    }
}
