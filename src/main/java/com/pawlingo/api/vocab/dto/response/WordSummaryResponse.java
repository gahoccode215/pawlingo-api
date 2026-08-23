package com.pawlingo.api.vocab.dto.response;

import com.pawlingo.api.vocab.enums.DifficultyLevel;
import com.pawlingo.api.vocab.enums.PartOfSpeech;
import java.util.UUID;

public record WordSummaryResponse(
        UUID id,
        String word,
        String phonetic,
        DifficultyLevel difficultyLevel,
        PartOfSpeech partOfSpeech,
        String primaryMeaning) {}
