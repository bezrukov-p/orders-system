package com.bezrukov.orderservice.exceptions;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends ApiException {
    public UsernameAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
