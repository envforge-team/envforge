CREATE TABLE users (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'OPERATOR', 'ADMIN'))
);

CREATE INDEX idx_users_email
    ON users(email);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100),
    result VARCHAR(20) NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_audit_events_result
        CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_audit_events_actor
    ON audit_events(actor);
CREATE INDEX idx_audit_events_resource
    ON audit_events(resource_type, resource_id);
CREATE INDEX idx_audit_events_created_at
    ON audit_events(created_at);
