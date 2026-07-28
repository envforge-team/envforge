CREATE TABLE environments (
    id UUID PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE,
    namespace VARCHAR(63) NOT NULL UNIQUE,
    template VARCHAR(50) NOT NULL,
    image_version VARCHAR(100) NOT NULL,
    replicas INTEGER NOT NULL,
    resource_profile VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    monitoring_enabled BOOLEAN NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_environment_name
        CHECK (name ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$'),

    CONSTRAINT chk_environment_replicas
        CHECK (replicas BETWEEN 1 AND 5),

    CONSTRAINT chk_environment_resource_profile
        CHECK (resource_profile IN ('SMALL', 'MEDIUM', 'LARGE')),

    CONSTRAINT chk_environment_status
        CHECK (
            status IN (
                'REQUESTED',
                'PROVISIONING',
                'DEPLOYING',
                'READY',
                'DEGRADED',
                'FAILED',
                'DELETING',
                'DELETED',
                'EXPIRED'
            )
        )
);

CREATE INDEX idx_environments_status
    ON environments(status);

CREATE INDEX idx_environments_expires_at
    ON environments(expires_at);

CREATE INDEX idx_environments_created_by
    ON environments(created_by);

CREATE TABLE environment_templates (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    image_repository VARCHAR(255) NOT NULL,
    default_image_version VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO environment_templates (
    id,
    code,
    display_name,
    image_repository,
    default_image_version,
    active,
    created_at
)
VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'STATIC_WEB',
    'Static Web App',
    'envforge/static-web-demo',
    '0.1.0',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    '22222222-2222-2222-2222-222222222222',
    'RELIABILITY_API',
    'Reliability Demo API',
    'envforge/reliability-demo-api',
    '0.1.0',
    TRUE,
    CURRENT_TIMESTAMP
);