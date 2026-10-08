package com.bezrukov.orderservice.service;

import com.bezrukov.orderservice.entity.Role;

import java.util.Set;

public interface RoleService {
    Set<Role> findRolesByNameIn(Set<String> roleNames);
}
