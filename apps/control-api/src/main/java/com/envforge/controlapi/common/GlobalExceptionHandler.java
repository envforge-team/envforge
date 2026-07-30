package com.envforge.controlapi.common;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import com.envforge.controlapi.environment.EnvironmentNotFoundException;
import com.envforge.controlapi.environment
    .EnvironmentAlreadyExistsException;
import com.envforge.controlapi.security
    .AccessDeniedException;
import com.envforge.controlapi.user
    .UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind
    .MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import com.envforge.controlapi.deployment.ConcurrentRolloutException;
import com.envforge.controlapi.deployment.DeploymentNotFoundException;
import com.envforge.controlapi.deployment.InvalidVersionException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EnvironmentAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleDuplicate(
        EnvironmentAlreadyExistsException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            request.getRequestURI(),
            Map.of()
        );
    }
    @ExceptionHandler(EnvironmentNotFoundException.class)
public ResponseEntity<ApiError> handleEnvironmentNotFound(
    EnvironmentNotFoundException exception,
    HttpServletRequest request
) {
    return buildResponse(
        HttpStatus.NOT_FOUND,
        exception.getMessage(),
        request.getRequestURI(),
        Map.of()
    );
}


 @ExceptionHandler(DeploymentNotFoundException.class)
public ResponseEntity<ApiError> handleDeploymentNotFound(
    DeploymentNotFoundException exception,
    HttpServletRequest request
) {
    return buildResponse(
        HttpStatus.NOT_FOUND,
        exception.getMessage(),
        request.getRequestURI(),
        Map.of()
    );
}

@ExceptionHandler(InvalidVersionException.class)
public ResponseEntity<ApiError> handleInvalidVersion(
    InvalidVersionException exception,
    HttpServletRequest request
) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        exception.getMessage(),
        request.getRequestURI(),
        Map.of()
    );
}

@ExceptionHandler(ConcurrentRolloutException.class)
public ResponseEntity<ApiError> handleConcurrentRollout(
    ConcurrentRolloutException exception,
    HttpServletRequest request
) {
    return buildResponse(
        HttpStatus.CONFLICT,
        exception.getMessage(),
        request.getRequestURI(),
        Map.of()
    );
}

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(
        UserNotFoundException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            exception.getMessage(),
            request.getRequestURI(),
            Map.of()
        );
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.FORBIDDEN,
            exception.getMessage(),
            request.getRequestURI(),
            Map.of()
        );
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> validationErrors =
            new LinkedHashMap<>();
        exception.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                validationErrors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            request.getRequestURI(),
            validationErrors
        );
    }
    private ResponseEntity<ApiError> buildResponse(
        HttpStatus status,
        String message,
        String path,
        Map<String, String> validationErrors
    ) {
        ApiError error = new ApiError(
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            Instant.now(),
            validationErrors
        );
        return ResponseEntity
            .status(status)
            .body(error);
    }
}
