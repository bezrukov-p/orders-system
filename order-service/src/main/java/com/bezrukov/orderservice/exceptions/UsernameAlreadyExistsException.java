package com.bezrukov.orderservice.exceptions;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends ApiException {
    public UsernameAlreadyExistsException(String username) {
        super(String.format("User with username '%s' already exists", username), HttpStatus.CONFLICT);
    }
}
