package com.pawlingo.api.vocab.service;

import com.pawlingo.api.vocab.dto.response.UserVocabularyResponse;

public record AddVocabularyResult(UserVocabularyResponse response, boolean created) {}
