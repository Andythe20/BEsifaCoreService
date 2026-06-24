package com.sifa.core_sifa.exception;

public class RestoreException extends RuntimeException {

    public RestoreException(String message) {
        super(message);
    }

    public RestoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
