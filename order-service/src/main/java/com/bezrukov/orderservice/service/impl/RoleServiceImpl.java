package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.entity.Role;
import com.bezrukov.orderservice.reposiroty.RoleRepository;
import com.bezrukov.orderservice.service.RoleService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    @Cacheable(value = "roles", keyGenerator = "sortedSetKeyGenerator")
    public Set<Role> findRolesByNameIn(Set<String> roleNames) {
        return roleRepository.findAllByNameIn(roleNames);
    }
}
