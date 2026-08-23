package com.pawlingo.api.vocab.repository;

import com.pawlingo.api.vocab.entity.WordExample;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordExampleRepository extends JpaRepository<WordExample, UUID> {

    List<WordExample> findByWordIdOrderByOrderIndexAsc(UUID wordId);
}
