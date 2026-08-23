CREATE TABLE user_vocabularies (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    word_id       UUID NOT NULL REFERENCES words(id),
    is_favorite   BOOLEAN NOT NULL DEFAULT false,
    status        VARCHAR(16) NOT NULL DEFAULT 'NEW',
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_vocabularies_user_id_word_id UNIQUE (user_id, word_id)
);
