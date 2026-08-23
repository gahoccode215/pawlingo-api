package com.pawlingo.api.vocab.service;

import com.pawlingo.api.vocab.dto.response.UserVocabularyResponse;
import com.pawlingo.api.vocab.enums.VocabularyStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserVocabularyService {

    AddVocabularyResult addVocabulary(UUID userId, UUID wordId);

    void removeVocabulary(UUID userId, UUID wordId);

    UserVocabularyResponse setFavorite(UUID userId, UUID wordId, boolean isFavorite);

    Page<UserVocabularyResponse> listUserVocabularies(
            UUID userId, Boolean isFavorite, VocabularyStatus status, Pageable pageable);
}
