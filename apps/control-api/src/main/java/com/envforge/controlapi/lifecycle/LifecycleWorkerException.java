package com.envforge.controlapi.lifecycle;

public class LifecycleWorkerException extends RuntimeException {

    private final int statusCode;

    public LifecycleWorkerException(
        int statusCode,
        String message
    ) {
        super(message);
        this.statusCode = statusCode;
    }

    public LifecycleWorkerException(
        int statusCode,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
