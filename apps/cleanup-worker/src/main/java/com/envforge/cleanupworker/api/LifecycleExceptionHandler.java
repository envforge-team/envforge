package com.envforge.cleanupworker.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LifecycleExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException exception) {
        ProblemDetail detail =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setTitle("Lifecycle conflict");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
