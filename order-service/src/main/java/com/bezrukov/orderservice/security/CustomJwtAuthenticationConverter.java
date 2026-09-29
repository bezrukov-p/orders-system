package com.bezrukov.orderservice.security;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CustomJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    @WithSpan("convert.jwt")
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaim("userId")));
        String username = jwt.getSubject();
        String email = jwt.getClaim("email");

        List<String> roles = jwt.getClaim("roles");
        assert roles != null;
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        CustomUserPrincipal principal = new CustomUserPrincipal(
                userId,
                username,
                email,
                authorities
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities
        );
    }
}
