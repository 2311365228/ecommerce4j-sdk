package com.ecommerce4j.api.exception;

public class EcommIntegrationException extends RuntimeException {
    public EcommIntegrationException(String message) {
        super(message);
    }

    public EcommIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
