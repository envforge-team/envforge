package com.envforge.cleanupworker.service;

public class InvalidLifecycleTransitionException extends RuntimeException {

    public InvalidLifecycleTransitionException(String message) {
        super(message);
    }
}
