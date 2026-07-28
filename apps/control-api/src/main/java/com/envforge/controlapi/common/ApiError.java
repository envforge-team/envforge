package com.envforge.controlapi.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp,
    Map<String, String> validationErrors
) {
}