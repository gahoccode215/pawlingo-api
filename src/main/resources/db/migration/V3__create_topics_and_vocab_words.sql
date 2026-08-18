CREATE TABLE topics (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(64) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    order_index   INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_topics_code UNIQUE (code)
);

CREATE TABLE vocab_words (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id          UUID NOT NULL REFERENCES topics(id),
    word              VARCHAR(255) NOT NULL,
    meaning           VARCHAR(255) NOT NULL,
    example_sentence  TEXT,
    image_url         VARCHAR(500),
    audio_url         VARCHAR(500),
    order_index       INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_vocab_words_topic_id ON vocab_words (topic_id);

INSERT INTO topics (id, code, name, description, order_index) VALUES
    ('11111111-1111-1111-1111-111111111111', 'animals', 'Animals', 'Common animals in English', 1),
    ('22222222-2222-2222-2222-222222222222', 'family', 'Family', 'Family members and relationships', 2);

INSERT INTO vocab_words (topic_id, word, meaning, example_sentence, order_index) VALUES
    ('11111111-1111-1111-1111-111111111111', 'dog', 'con chó', 'The dog is running in the park.', 1),
    ('11111111-1111-1111-1111-111111111111', 'cat', 'con mèo', 'The cat is sleeping on the sofa.', 2),
    ('11111111-1111-1111-1111-111111111111', 'bird', 'con chim', 'The bird is singing in the tree.', 3),
    ('11111111-1111-1111-1111-111111111111', 'fish', 'con cá', 'The fish is swimming in the pond.', 4),
    ('11111111-1111-1111-1111-111111111111', 'horse', 'con ngựa', 'The horse is running across the field.', 5),
    ('11111111-1111-1111-1111-111111111111', 'cow', 'con bò', 'The cow gives us milk.', 6),
    ('11111111-1111-1111-1111-111111111111', 'pig', 'con lợn', 'The pig is eating in the mud.', 7),
    ('11111111-1111-1111-1111-111111111111', 'sheep', 'con cừu', 'The sheep gives us wool.', 8),
    ('11111111-1111-1111-1111-111111111111', 'chicken', 'con gà', 'The chicken laid an egg.', 9),
    ('11111111-1111-1111-1111-111111111111', 'duck', 'con vịt', 'The duck is swimming in the lake.', 10),
    ('11111111-1111-1111-1111-111111111111', 'rabbit', 'con thỏ', 'The rabbit is hopping in the garden.', 11),
    ('11111111-1111-1111-1111-111111111111', 'elephant', 'con voi', 'The elephant is the largest land animal.', 12),
    ('11111111-1111-1111-1111-111111111111', 'lion', 'sư tử', 'The lion is the king of the jungle.', 13),
    ('11111111-1111-1111-1111-111111111111', 'tiger', 'con hổ', 'The tiger has orange and black stripes.', 14),
    ('11111111-1111-1111-1111-111111111111', 'monkey', 'con khỉ', 'The monkey is climbing the tree.', 15),
    ('22222222-2222-2222-2222-222222222222', 'mother', 'mẹ', 'My mother cooks dinner every night.', 1),
    ('22222222-2222-2222-2222-222222222222', 'father', 'bố', 'My father drives me to school.', 2),
    ('22222222-2222-2222-2222-222222222222', 'sister', 'chị/em gái', 'My sister is younger than me.', 3),
    ('22222222-2222-2222-2222-222222222222', 'brother', 'anh/em trai', 'My brother plays football every weekend.', 4),
    ('22222222-2222-2222-2222-222222222222', 'grandmother', 'bà', 'My grandmother tells great stories.', 5),
    ('22222222-2222-2222-2222-222222222222', 'grandfather', 'ông', 'My grandfather likes to read newspapers.', 6),
    ('22222222-2222-2222-2222-222222222222', 'aunt', 'cô/dì', 'My aunt visits us every summer.', 7),
    ('22222222-2222-2222-2222-222222222222', 'uncle', 'chú/bác/cậu', 'My uncle works as a doctor.', 8),
    ('22222222-2222-2222-2222-222222222222', 'cousin', 'anh chị em họ', 'My cousin lives in another city.', 9),
    ('22222222-2222-2222-2222-222222222222', 'son', 'con trai', 'Their son just started school.', 10),
    ('22222222-2222-2222-2222-222222222222', 'daughter', 'con gái', 'Their daughter loves to paint.', 11),
    ('22222222-2222-2222-2222-222222222222', 'husband', 'chồng', 'Her husband works from home.', 12),
    ('22222222-2222-2222-2222-222222222222', 'wife', 'vợ', 'His wife teaches at a school.', 13),
    ('22222222-2222-2222-2222-222222222222', 'parents', 'bố mẹ', 'My parents always support me.', 14),
    ('22222222-2222-2222-2222-222222222222', 'family', 'gia đình', 'I love spending time with my family.', 15);
