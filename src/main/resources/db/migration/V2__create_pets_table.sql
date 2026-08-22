CREATE TABLE pets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    stage         INT NOT NULL DEFAULT 1,
    xp            INT NOT NULL DEFAULT 0,
    energy        INT NOT NULL DEFAULT 100,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_pets_user_id UNIQUE (user_id)
);
