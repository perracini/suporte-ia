package com.eneng.suporte.domain.exception;

public class LlamaUnavailableException extends DomainException {
    public LlamaUnavailableException(String message) {
        super(message);
    }

    public LlamaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
