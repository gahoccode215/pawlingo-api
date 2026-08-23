package com.pawlingo.api.vocab.dto.response;

import com.pawlingo.api.vocab.enums.DifficultyLevel;
import com.pawlingo.api.vocab.enums.PartOfSpeech;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WordDetailResponse(
        UUID id,
        String word,
        String phonetic,
        String audioUrl,
        DifficultyLevel difficultyLevel,
        PartOfSpeech partOfSpeech,
        String primaryMeaning,
        Instant createdAt,
        Instant updatedAt,
        List<WordExampleResponse> examples) {}
