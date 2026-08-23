package com.pawlingo.api.vocab.service;

import com.pawlingo.api.vocab.dto.response.WordDetailResponse;
import com.pawlingo.api.vocab.dto.response.WordSummaryResponse;
import com.pawlingo.api.vocab.enums.DifficultyLevel;
import com.pawlingo.api.vocab.enums.PartOfSpeech;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VocabularyService {

    Page<WordSummaryResponse> listWords(
            String q, DifficultyLevel difficultyLevel, PartOfSpeech partOfSpeech, Pageable pageable);

    WordDetailResponse getWordDetail(UUID wordId);
}
