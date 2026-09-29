package com.bezrukov.orderservice.service;

import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface ApplicationUserDetailsService extends UserDetailsService {
    @NonNull
    UserDetails loadUserByUsername(@NonNull String username);
}
