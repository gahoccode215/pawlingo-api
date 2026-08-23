INSERT INTO words (id, word, normalized_word, phonetic, difficulty_level, part_of_speech, primary_meaning) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'environment', 'environment', '/ɪnˈvaɪrənmənt/', 'B1', 'NOUN', 'The natural world, as it relates to living organisms.'),
    ('a0000000-0000-0000-0000-000000000002', 'book', 'book', '/bʊk/', 'A1', 'NOUN', 'A written or printed work consisting of pages bound together.'),
    ('a0000000-0000-0000-0000-000000000003', 'book', 'book', '/bʊk/', 'A2', 'VERB', 'To reserve a place, ticket, or service in advance.'),
    ('a0000000-0000-0000-0000-000000000004', 'run', 'run', '/rʌn/', 'A1', 'VERB', 'To move at a speed faster than walking, never having both feet on the ground at once.'),
    ('a0000000-0000-0000-0000-000000000005', 'beautiful', 'beautiful', '/ˈbjuːtɪfʊl/', 'A1', 'ADJECTIVE', 'Pleasing the senses or mind aesthetically.'),
    ('a0000000-0000-0000-0000-000000000006', 'quickly', 'quickly', '/ˈkwɪkli/', 'A1', 'ADVERB', 'At a fast speed; rapidly.'),
    ('a0000000-0000-0000-0000-000000000007', 'because', 'because', '/bɪˈkɒz/', 'A1', 'CONJUNCTION', 'For the reason that; since.'),
    ('a0000000-0000-0000-0000-000000000008', 'wow', 'wow', '/waʊ/', 'A1', 'INTERJECTION', 'Used to express strong feelings of surprise, admiration, or pleasure.'),
    ('a0000000-0000-0000-0000-000000000009', 'through', 'through', '/θruː/', 'A2', 'PREPOSITION', 'Moving in one side and out of the other side of an opening or location.'),
    ('a0000000-0000-0000-0000-00000000000a', 'sustainability', 'sustainability', '/səˌsteɪnəˈbɪləti/', 'C1', 'NOUN', 'The ability to be maintained at a certain rate or level without depleting resources.'),
    ('a0000000-0000-0000-0000-00000000000b', 'ubiquitous', 'ubiquitous', '/juːˈbɪkwɪtəs/', 'C2', 'ADJECTIVE', 'Present, appearing, or found everywhere.');

INSERT INTO word_examples (word_id, sentence, translation, source, order_index) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'We must protect the environment for future generations.', NULL, 'Oxford Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000001', 'Plastic waste is harmful to the marine environment.', NULL, 'Oxford Dictionary', 1),
    ('a0000000-0000-0000-0000-000000000002', 'She read the book in one afternoon.', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000003', 'I booked a table for two at the restaurant.', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000004', 'He runs five kilometers every morning.', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000005', 'The sunset over the mountains was beautiful.', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000006', 'She quickly finished her homework before dinner.', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000007', 'I stayed home because it was raining.', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000008', 'Wow, that firework display was amazing!', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-000000000009', 'The path goes through the forest.', NULL, 'Cambridge Dictionary', 0),
    ('a0000000-0000-0000-0000-00000000000a', 'The company invested in sustainability initiatives.', NULL, 'Oxford Dictionary', 0),
    ('a0000000-0000-0000-0000-00000000000b', 'Smartphones have become ubiquitous in modern life.', NULL, 'Oxford Dictionary', 0);
