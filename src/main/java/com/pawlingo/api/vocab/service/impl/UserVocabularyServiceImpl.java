package com.pawlingo.api.vocab.service.impl;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.vocab.dto.response.UserVocabularyResponse;
import com.pawlingo.api.vocab.dto.response.WordSummaryResponse;
import com.pawlingo.api.vocab.entity.UserVocabulary;
import com.pawlingo.api.vocab.entity.Word;
import com.pawlingo.api.vocab.enums.VocabularyStatus;
import com.pawlingo.api.vocab.repository.UserVocabularyRepository;
import com.pawlingo.api.vocab.repository.WordRepository;
import com.pawlingo.api.vocab.service.AddVocabularyResult;
import com.pawlingo.api.vocab.service.UserVocabularyService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserVocabularyServiceImpl implements UserVocabularyService {

    private final UserVocabularyRepository userVocabularyRepository;
    private final WordRepository wordRepository;

    public UserVocabularyServiceImpl(UserVocabularyRepository userVocabularyRepository, WordRepository wordRepository) {
        this.userVocabularyRepository = userVocabularyRepository;
        this.wordRepository = wordRepository;
    }

    @Override
    @Transactional
    public AddVocabularyResult addVocabulary(UUID userId, UUID wordId) {
        if (!wordRepository.existsById(wordId)) {
            throw new BusinessException(ErrorCode.WORD_NOT_FOUND);
        }
        Optional<UserVocabulary> existing = userVocabularyRepository.findByUserIdAndWordId(userId, wordId);
        if (existing.isPresent()) {
            return new AddVocabularyResult(toResponse(existing.get(), null), false);
        }
        UserVocabulary saved = userVocabularyRepository.save(UserVocabulary.builder()
                .userId(userId)
                .wordId(wordId)
                .favorite(false)
                .status(VocabularyStatus.NEW)
                .build());
        return new AddVocabularyResult(toResponse(saved, null), true);
    }

    @Override
    @Transactional
    public void removeVocabulary(UUID userId, UUID wordId) {
        UserVocabulary entry = userVocabularyRepository
                .findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOCABULARY_NOT_FOUND));
        userVocabularyRepository.delete(entry);
    }

    @Override
    @Transactional
    public UserVocabularyResponse setFavorite(UUID userId, UUID wordId, boolean isFavorite) {
        if (!wordRepository.existsById(wordId)) {
            throw new BusinessException(ErrorCode.WORD_NOT_FOUND);
        }
        UserVocabulary entry = userVocabularyRepository
                .findByUserIdAndWordId(userId, wordId)
                .orElseGet(() -> UserVocabulary.builder()
                        .userId(userId)
                        .wordId(wordId)
                        .status(VocabularyStatus.NEW)
                        .build());
        entry.setFavorite(isFavorite);
        return toResponse(userVocabularyRepository.save(entry), null);
    }

    @Override
    public Page<UserVocabularyResponse> listUserVocabularies(
            UUID userId, Boolean isFavorite, VocabularyStatus status, Pageable pageable) {
        Page<UserVocabulary> page = userVocabularyRepository.search(userId, isFavorite, status, pageable);
        List<UUID> wordIds = page.getContent().stream().map(UserVocabulary::getWordId).toList();
        Map<UUID, WordSummaryResponse> wordsById = wordRepository.findAllById(wordIds).stream()
                .collect(Collectors.toMap(Word::getId, this::toWordSummary));
        return page.map(entry -> toResponse(entry, wordsById.get(entry.getWordId())));
    }

    private WordSummaryResponse toWordSummary(Word word) {
        return new WordSummaryResponse(
                word.getId(),
                word.getWord(),
                word.getPhonetic(),
                word.getDifficultyLevel(),
                word.getPartOfSpeech(),
                word.getPrimaryMeaning());
    }

    private UserVocabularyResponse toResponse(UserVocabulary entry, WordSummaryResponse word) {
        return new UserVocabularyResponse(
                entry.getId(), entry.getWordId(), entry.isFavorite(), entry.getStatus(), entry.getCreatedAt(), word);
    }
}
