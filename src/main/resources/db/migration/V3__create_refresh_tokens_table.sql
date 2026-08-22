CREATE TABLE refresh_tokens (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL REFERENCES users(id),
    token_hash            VARCHAR(64) NOT NULL,
    issued_at             TIMESTAMP NOT NULL DEFAULT now(),
    expires_at            TIMESTAMP NOT NULL,
    revoked_at            TIMESTAMP,
    replaced_by_token_id  UUID REFERENCES refresh_tokens(id),
    user_agent            VARCHAR(255),
    ip_address            VARCHAR(64),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
