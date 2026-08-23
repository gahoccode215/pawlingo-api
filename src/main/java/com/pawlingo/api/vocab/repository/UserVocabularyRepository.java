package com.pawlingo.api.vocab.repository;

import com.pawlingo.api.vocab.entity.UserVocabulary;
import com.pawlingo.api.vocab.enums.VocabularyStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, UUID> {

    Optional<UserVocabulary> findByUserIdAndWordId(UUID userId, UUID wordId);

    @Query(
            """
            SELECT uv FROM UserVocabulary uv
            WHERE uv.userId = :userId
            AND (:isFavorite IS NULL OR uv.favorite = :isFavorite)
            AND (:status IS NULL OR uv.status = :status)
            """)
    Page<UserVocabulary> search(
            @Param("userId") UUID userId,
            @Param("isFavorite") Boolean isFavorite,
            @Param("status") VocabularyStatus status,
            Pageable pageable);
}
