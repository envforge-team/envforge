CREATE TABLE environments (
    id UUID PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    namespace VARCHAR(63) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE lifecycle_job (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    target_revision INTEGER,
    actor_id VARCHAR(255) NOT NULL,
    namespace_name VARCHAR(255),
    helm_release_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_lifecycle_job_status_retry
    ON lifecycle_job (status, next_retry_at);

CREATE INDEX idx_lifecycle_job_environment
    ON lifecycle_job (environment_id);

CREATE TABLE lifecycle_audit (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL,
    job_id UUID,
    actor_id VARCHAR(255) NOT NULL,
    action VARCHAR(40) NOT NULL,
    result VARCHAR(40) NOT NULL,
    details VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_lifecycle_audit_environment
    ON lifecycle_audit (environment_id, created_at);
