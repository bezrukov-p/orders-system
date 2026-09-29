package com.bezrukov.orderservice.reposiroty;

import com.bezrukov.orderservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Set<Role> findAllByNameIn(Set<String> names);
}
