CREATE TABLE word_examples (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    word_id       UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    sentence      VARCHAR(500) NOT NULL,
    translation   VARCHAR(500),
    source        VARCHAR(200),
    order_index   INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_word_examples_word_id ON word_examples (word_id);
