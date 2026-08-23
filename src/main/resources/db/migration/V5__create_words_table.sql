CREATE TABLE words (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    word              VARCHAR(100) NOT NULL,
    normalized_word   VARCHAR(100) NOT NULL,
    phonetic          VARCHAR(100),
    audio_url         VARCHAR(500),
    difficulty_level  VARCHAR(2),
    part_of_speech    VARCHAR(32) NOT NULL,
    primary_meaning   VARCHAR(500) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_words_normalized_word_part_of_speech UNIQUE (normalized_word, part_of_speech)
);

CREATE INDEX idx_words_difficulty_level ON words (difficulty_level);
CREATE INDEX idx_words_part_of_speech ON words (part_of_speech);
