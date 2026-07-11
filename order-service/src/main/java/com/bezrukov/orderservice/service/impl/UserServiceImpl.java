package com.bezrukov.orderservice.service.impl;

import com.bezrukov.orderservice.dto.RegisterRequest;
import com.bezrukov.orderservice.entity.Role;
import com.bezrukov.orderservice.entity.User;
import com.bezrukov.orderservice.exceptions.UsernameAlreadyExistsException;
import com.bezrukov.orderservice.reposiroty.UserRepository;
import com.bezrukov.orderservice.service.ApplicationUserDetailsService;
import com.bezrukov.orderservice.service.RoleService;
import com.bezrukov.orderservice.service.UserService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Qualifier("mainUserService")
@Slf4j
public class UserServiceImpl implements UserService, ApplicationUserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException(username);
        }
        User user = userOpt.get();

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toUnmodifiableList());
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

    @Override
    public User create(RegisterRequest registerRequest, Set<String> roles) {
        String username = registerRequest.username();
        if (userRepository.existsByUsername(username)) {
            log.warn("Username already exists: {}", username);
            throw new UsernameAlreadyExistsException("Username " + username + " already exists");
        }

        Set<Role> rolesToAttach = roleService.findRolesByNameIn(roles);
        if (rolesToAttach.size() != roles.size()) {
            log.warn("Roles do not match");
            throw new IllegalArgumentException("some roles not found in the system");
        }

        User user = userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(registerRequest.password()))
                .email(registerRequest.email())
                .roles(rolesToAttach)
                .build());
        log.info("User created: {}", user.getUsername());
        return user;
    }

    @Override
    public Optional<User> getUser(String username) {
        return userRepository.findByUsername(username);
    }
}
