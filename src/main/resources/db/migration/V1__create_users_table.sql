CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255),
    goal           VARCHAR(32) NOT NULL DEFAULT 'BEGINNER',
    auth_provider  VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    google_id      VARCHAR(255),
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_google_id UNIQUE (google_id)
);
