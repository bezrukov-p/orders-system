package com.bezrukov.orderservice.service.impl;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Component("sortedSetKeyGenerator")
public class SortedSetKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        return Arrays.stream(params)
                .map(p -> {
                    if (p instanceof Set<?> set) {
                        return set.stream().sorted().toList();
                    }
                    return p;
                })
                .toList();
    }
}
