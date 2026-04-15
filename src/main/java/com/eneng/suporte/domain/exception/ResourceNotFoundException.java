package com.eneng.suporte.domain.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resource, Object id) {
        super("%s nao encontrado: %s".formatted(resource, id));
    }
}
