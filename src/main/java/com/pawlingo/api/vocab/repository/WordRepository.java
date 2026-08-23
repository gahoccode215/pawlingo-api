package com.pawlingo.api.vocab.repository;

import com.pawlingo.api.vocab.entity.Word;
import com.pawlingo.api.vocab.enums.DifficultyLevel;
import com.pawlingo.api.vocab.enums.PartOfSpeech;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WordRepository extends JpaRepository<Word, UUID> {

    @Query(
            """
            SELECT w FROM Word w
            WHERE (:normalizedPrefix IS NULL OR w.normalizedWord LIKE CONCAT(CAST(:normalizedPrefix AS string), '%'))
            AND (:difficultyLevel IS NULL OR w.difficultyLevel = :difficultyLevel)
            AND (:partOfSpeech IS NULL OR w.partOfSpeech = :partOfSpeech)
            """)
    Page<Word> search(
            @Param("normalizedPrefix") String normalizedPrefix,
            @Param("difficultyLevel") DifficultyLevel difficultyLevel,
            @Param("partOfSpeech") PartOfSpeech partOfSpeech,
            Pageable pageable);
}
