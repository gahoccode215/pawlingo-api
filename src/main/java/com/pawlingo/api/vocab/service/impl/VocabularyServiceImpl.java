package com.pawlingo.api.vocab.service.impl;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.vocab.dto.response.WordDetailResponse;
import com.pawlingo.api.vocab.dto.response.WordExampleResponse;
import com.pawlingo.api.vocab.dto.response.WordSummaryResponse;
import com.pawlingo.api.vocab.entity.Word;
import com.pawlingo.api.vocab.entity.WordExample;
import com.pawlingo.api.vocab.enums.DifficultyLevel;
import com.pawlingo.api.vocab.enums.PartOfSpeech;
import com.pawlingo.api.vocab.repository.WordExampleRepository;
import com.pawlingo.api.vocab.repository.WordRepository;
import com.pawlingo.api.vocab.service.VocabularyService;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class VocabularyServiceImpl implements VocabularyService {

    private final WordRepository wordRepository;
    private final WordExampleRepository wordExampleRepository;

    public VocabularyServiceImpl(WordRepository wordRepository, WordExampleRepository wordExampleRepository) {
        this.wordRepository = wordRepository;
        this.wordExampleRepository = wordExampleRepository;
    }

    @Override
    public Page<WordSummaryResponse> listWords(
            String q, DifficultyLevel difficultyLevel, PartOfSpeech partOfSpeech, Pageable pageable) {
        if (q != null && q.trim().length() < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "q must be at least 2 characters");
        }
        String normalizedPrefix = (q == null || q.isBlank()) ? null : normalize(q);
        return wordRepository
                .search(normalizedPrefix, difficultyLevel, partOfSpeech, pageable)
                .map(this::toSummary);
    }

    @Override
    public WordDetailResponse getWordDetail(UUID wordId) {
        Word word = wordRepository.findById(wordId).orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));
        List<WordExampleResponse> examples = wordExampleRepository.findByWordIdOrderByOrderIndexAsc(wordId).stream()
                .map(this::toExample)
                .toList();
        return new WordDetailResponse(
                word.getId(),
                word.getWord(),
                word.getPhonetic(),
                word.getAudioUrl(),
                word.getDifficultyLevel(),
                word.getPartOfSpeech(),
                word.getPrimaryMeaning(),
                word.getCreatedAt(),
                word.getUpdatedAt(),
                examples);
    }

    private WordSummaryResponse toSummary(Word word) {
        return new WordSummaryResponse(
                word.getId(),
                word.getWord(),
                word.getPhonetic(),
                word.getDifficultyLevel(),
                word.getPartOfSpeech(),
                word.getPrimaryMeaning());
    }

    private WordExampleResponse toExample(WordExample example) {
        return new WordExampleResponse(
                example.getId(), example.getSentence(), example.getTranslation(), example.getSource(), example.getOrderIndex());
    }

    private static String normalize(String raw) {
        String trimmedLower = raw.trim().toLowerCase();
        String decomposed = Normalizer.normalize(trimmedLower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }
}
