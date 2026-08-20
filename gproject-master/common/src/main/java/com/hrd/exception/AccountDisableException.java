package com.hrd.exception;

public class AccountDisableException extends RuntimeException {
    public AccountDisableException() {
    }
    public AccountDisableException(String message) {
        super(message);
    }
}
