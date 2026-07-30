CREATE TABLE environment_events (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    source VARCHAR(100) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_environment_events_environment
        FOREIGN KEY (environment_id)
        REFERENCES environments(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_environment_event_type
        CHECK (
            event_type IN (
                'CREATED',
                'DEPLOYMENT_STARTED',
                'DEPLOYMENT_SUCCEEDED',
                'DEPLOYMENT_FAILED',
                'POD_RESTARTED',
                'HEALTH_DEGRADED',
                'HEALTH_RECOVERED',
                'EXPIRED',
                'DELETED'
            )
        ),

    CONSTRAINT chk_environment_event_severity
        CHECK (severity IN ('INFO', 'WARNING', 'ERROR'))
);

CREATE INDEX idx_environment_events_environment
    ON environment_events(environment_id);

CREATE INDEX idx_environment_events_occurred_at
    ON environment_events(occurred_at);

CREATE INDEX idx_environment_events_environment_time
    ON environment_events(environment_id, occurred_at DESC);


CREATE TABLE health_snapshots (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    cpu_usage_percent DOUBLE PRECISION,
    memory_usage_bytes BIGINT,
    request_rate_per_second DOUBLE PRECISION,
    error_rate_percent DOUBLE PRECISION,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_health_snapshots_environment
        FOREIGN KEY (environment_id)
        REFERENCES environments(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_health_snapshot_status
        CHECK (
            status IN (
                'HEALTHY',
                'DEGRADED',
                'UNHEALTHY',
                'UNKNOWN'
            )
        ),

    CONSTRAINT chk_health_cpu_usage
        CHECK (
            cpu_usage_percent IS NULL
            OR cpu_usage_percent BETWEEN 0 AND 100
        ),

    CONSTRAINT chk_health_memory_usage
        CHECK (
            memory_usage_bytes IS NULL
            OR memory_usage_bytes >= 0
        ),

    CONSTRAINT chk_health_request_rate
        CHECK (
            request_rate_per_second IS NULL
            OR request_rate_per_second >= 0
        ),

    CONSTRAINT chk_health_error_rate
        CHECK (
            error_rate_percent IS NULL
            OR error_rate_percent BETWEEN 0 AND 100
        )
);

CREATE INDEX idx_health_snapshots_environment
    ON health_snapshots(environment_id);

CREATE INDEX idx_health_snapshots_captured_at
    ON health_snapshots(captured_at);

CREATE INDEX idx_health_snapshots_environment_time
    ON health_snapshots(environment_id, captured_at DESC);
