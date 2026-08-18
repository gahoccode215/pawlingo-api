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

CREATE TABLE progress (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    vocab_word_id    UUID NOT NULL REFERENCES vocab_words(id),
    activity_type    VARCHAR(32) NOT NULL,
    correct          BOOLEAN NOT NULL,
    xp_earned        INT NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_progress_user_id ON progress (user_id);
CREATE INDEX idx_progress_vocab_word_id ON progress (vocab_word_id);
