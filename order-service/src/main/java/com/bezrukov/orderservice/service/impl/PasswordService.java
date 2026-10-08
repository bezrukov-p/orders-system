package com.bezrukov.orderservice.service.impl;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordService {
    private final PasswordEncoder passwordEncoder;

    @WithSpan("bcrypt.encode")
    public String encode(@SpanAttribute("password.length") String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
