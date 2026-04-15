package com.eneng.suporte.domain.exception;

public class RateLimitExceededException extends DomainException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
