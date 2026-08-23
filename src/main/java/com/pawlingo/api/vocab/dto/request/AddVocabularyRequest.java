package com.pawlingo.api.vocab.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddVocabularyRequest(@NotNull(message = "wordId is required") UUID wordId) {}
