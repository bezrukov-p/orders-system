package com.bezrukov.inventoryservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@Slf4j
public class AdminKeyInterceptor implements HandlerInterceptor {
    private final String adminApiKey;

    public AdminKeyInterceptor(@Value("${admin.apikey}") String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (request.getRequestURI().contains("/swagger-ui") ||
                request.getRequestURI().contains("/api-docs")) {
            return true;
        }

        String apiKey = request.getHeader("X-Admin-Key");
        if (apiKey != null && apiKey.equals(adminApiKey)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        try {
            response.getWriter().write("{\"error\":\"Invalid admin key\"}");
        } catch (IOException e) {
            log.warn("invalid admin key, ", e);
        }
        return false;
    }
}
