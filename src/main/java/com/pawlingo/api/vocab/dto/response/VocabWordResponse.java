package com.pawlingo.api.vocab.dto.response;

import java.util.UUID;

public record VocabWordResponse(
        UUID id,
        String word,
        String meaning,
        String exampleSentence,
        String imageUrl,
        String audioUrl,
        int orderIndex) {}
