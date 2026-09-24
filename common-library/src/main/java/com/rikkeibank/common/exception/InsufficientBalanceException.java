package com.rikkeibank.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends ApiException {
    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
