package com.pawlingo.api.vocab.repository;

import com.pawlingo.api.vocab.entity.VocabWord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabWordRepository extends JpaRepository<VocabWord, UUID> {

    List<VocabWord> findByTopicIdOrderByOrderIndexAsc(UUID topicId);

    long countByTopicId(UUID topicId);
}
