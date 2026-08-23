package com.pawlingo.api.vocab.dto.response;

import com.pawlingo.api.vocab.enums.VocabularyStatus;
import java.time.Instant;
import java.util.UUID;

public record UserVocabularyResponse(
        UUID id,
        UUID wordId,
        boolean isFavorite,
        VocabularyStatus status,
        Instant createdAt,
        WordSummaryResponse word) {}
