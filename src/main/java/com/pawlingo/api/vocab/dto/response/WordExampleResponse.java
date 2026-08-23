package com.pawlingo.api.vocab.dto.response;

import java.util.UUID;

public record WordExampleResponse(UUID id, String sentence, String translation, String source, int orderIndex) {}
