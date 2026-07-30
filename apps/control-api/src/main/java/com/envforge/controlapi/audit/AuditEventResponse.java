package com.envforge.controlapi.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    String actor,
    String action,
    String resourceType,
    String resourceId,
    AuditResult result,
    String details,
    Instant createdAt
) {
    public static AuditEventResponse from(AuditEventEntity event) {
        return new AuditEventResponse(
            event.getId(),
            event.getActor(),
            event.getAction(),
            event.getResourceType(),
            event.getResourceId(),
            event.getResult(),
            event.getDetails(),
            event.getCreatedAt()
        );
    }
}
