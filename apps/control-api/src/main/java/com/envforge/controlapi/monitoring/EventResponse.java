package com.envforge.controlapi.monitoring;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    UUID environmentId,
    EnvironmentEventType eventType,
    EventSeverity severity,
    String source,
    String message,
    Instant occurredAt
) {
    public static EventResponse from(
        EnvironmentEventEntity event
    ) {
        return new EventResponse(
            event.getId(),
            event.getEnvironmentId(),
            event.getEventType(),
            event.getSeverity(),
            event.getSource(),
            event.getMessage(),
            event.getOccurredAt()
        );
    }
}
