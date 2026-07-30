package com.envforge.controlapi.monitoring;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "environment_events")
public class EnvironmentEventEntity {

    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EnvironmentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventSeverity severity;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected EnvironmentEventEntity() {
    }

    public EnvironmentEventEntity(
        UUID id,
        UUID environmentId,
        EnvironmentEventType eventType,
        EventSeverity severity,
        String source,
        String message,
        Instant occurredAt
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.eventType = eventType;
        this.severity = severity;
        this.source = source;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public EnvironmentEventType getEventType() {
        return eventType;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
