package com.bezrukov.orderservice.utils;

import com.bezrukov.orderservice.dto.OrderItemResponse;
import com.bezrukov.orderservice.dto.OrderResponse;
import com.bezrukov.orderservice.dto.UserDto;
import com.bezrukov.orderservice.entity.Order;
import com.bezrukov.orderservice.entity.Role;
import com.bezrukov.orderservice.entity.User;

import java.util.UUID;
import java.util.stream.Collectors;

public class MapperDto {
    public static UserDto userToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }

    public static OrderResponse toOrderResponse(Order order, UUID userId) {
        return new OrderResponse(
                order.getId(),
                userId,
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                order.getItems().stream().map(orderItem -> new OrderItemResponse(
                        orderItem.getId(),
                        orderItem.getProductId(),
                        orderItem.getName(),
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getSalePercent()
                )).toList()
        );
    }
}
