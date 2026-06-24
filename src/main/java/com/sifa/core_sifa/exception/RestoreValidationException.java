package com.sifa.core_sifa.exception;

public class RestoreValidationException extends RestoreException {

    public RestoreValidationException(String message) {
        super(message);
    }

    public RestoreValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
