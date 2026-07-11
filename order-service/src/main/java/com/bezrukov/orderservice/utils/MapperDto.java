package com.bezrukov.orderservice.utils;

import com.bezrukov.orderservice.dto.UserDto;
import com.bezrukov.orderservice.entity.Role;
import com.bezrukov.orderservice.entity.User;

import java.util.stream.Collectors;

public class MapperDto {
    public static UserDto userToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }
}
