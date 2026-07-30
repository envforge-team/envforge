CREATE TABLE deployment (
    id BIGSERIAL PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environments(id),
    requested_version VARCHAR(50) NOT NULL,
    image_tag VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    triggered_by VARCHAR(100),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    failure_reason TEXT
);