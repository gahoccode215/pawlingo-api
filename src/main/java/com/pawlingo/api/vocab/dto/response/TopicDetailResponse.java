package com.pawlingo.api.vocab.dto.response;

import java.util.List;

public record TopicDetailResponse(String code, String name, String description, List<VocabWordResponse> words) {}
